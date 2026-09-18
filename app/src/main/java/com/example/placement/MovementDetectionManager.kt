package com.example.placement

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.camera.PlacementCameraManager
import com.example.domain.model.MovementSensitivity
import com.example.domain.model.MovementState
import com.example.sensors.OrientationSensorManager
import com.example.sensors.SensorReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Robust Phone Placement Detection Manager for FocusForest.
 *
 * Implements:
 * 1. Multi-sample calibration (2.5s) with stability & variance validation.
 * 2. Immutable reference state that remains locked for the entire session.
 * 3. Sensor fusion: 3D tilt from gravity unit vector, circular rotation angle from orientation,
 *    gyroscope stillness verification, and secondary camera visual luminance grid.
 * 4. Hysteresis: Stricter movement threshold vs tighter return threshold.
 * 5. Debouncing for movement (prevents false triggers on light taps/desk bumps).
 * 6. 1.2-second stillness confirmation period for return-to-place (RETURNING -> NORMAL).
 * 7. Explicit state machine: IDLE -> CALIBRATING -> NORMAL <-> MOVED -> RETURNING -> NORMAL -> SESSION_ENDED.
 * 8. Real-time diagnostics for Developer/Testing Mode.
 */
class MovementDetectionManager(
    private val context: Context,
    private val sensorManager: OrientationSensorManager,
    private val cameraManager: PlacementCameraManager,
    private val coroutineScope: CoroutineScope
) {

    private val _movementState = MutableStateFlow(MovementState.IDLE)
    val movementState: StateFlow<MovementState> = _movementState.asStateFlow()

    private val _calibrationProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val calibrationProgress: StateFlow<Float> = _calibrationProgress.asStateFlow()

    private val _returnProgress = MutableStateFlow(0f) // 0.0 to 1.0 during RETURNING state
    val returnProgress: StateFlow<Float> = _returnProgress.asStateFlow()

    private val _currentDelta = MutableStateFlow(0f)
    val currentDelta: StateFlow<Float> = _currentDelta.asStateFlow()

    private val _movementEventsCount = MutableStateFlow(0)
    val movementEventsCount: StateFlow<Int> = _movementEventsCount.asStateFlow()

    private val _diagnostics = MutableStateFlow(PlacementDiagnostics())
    val diagnostics: StateFlow<PlacementDiagnostics> = _diagnostics.asStateFlow()

    var sensitivity: MovementSensitivity = MovementSensitivity.MEDIUM
    var isVibrationEnabled: Boolean = true

    // The calibrated reference state remains unchanged for the whole session
    private var referenceState: PlacementReferenceState? = null

    private var monitorJob: Job? = null
    private var calibrationJob: Job? = null

    private var consecutiveMovedFrames = 0
    private var consecutiveReturnFrames = 0

    // Configuration constants
    private val moveConfirmationFrames = 3       // ~360ms of sustained deviation required to trigger MOVED
    private val returnConfirmationFrames = 12    // ~1440ms of continuous stillness & alignment required to confirm NORMAL
    private val monitorLoopIntervalMs = 120L

    // Gyroscope stillness limit (rad/s): phone must be stationary (< 0.25 rad/s) to qualify as resting
    private val maxRestingAngularVelocity = 0.25f

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Calibrates baseline phone position by taking multiple samples over [durationMs].
     * Verifies stability during calibration: if the user picks up or moves the phone
     * during baseline capture, calibration fails with MovementState.ERROR.
     */
    fun startCalibration(
        durationMs: Long = 2500L,
        onCalibrationComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!sensorManager.hasRequiredSensors()) {
            _movementState.value = MovementState.ERROR
            onError("Required motion sensors are not available on this device.")
            return
        }

        stopMonitoring()
        _movementState.value = MovementState.CALIBRATING
        _calibrationProgress.value = 0f
        _returnProgress.value = 0f
        sensorManager.startListening()

        calibrationJob = coroutineScope.launch(Dispatchers.Default) {
            val steps = 25
            val stepDelay = durationMs / steps
            val samples = mutableListOf<SensorReading>()

            for (i in 1..steps) {
                delay(stepDelay)
                _calibrationProgress.value = i.toFloat() / steps
                samples.add(sensorManager.readingState.value)
            }

            if (samples.size >= 10) {
                // 1. Verify stability during calibration
                val maxGyroDuringCal = samples.maxOfOrNull {
                    OrientationSensorManager.getGyroMagnitude(it)
                } ?: 0f

                // Calculate variance of gravity vector
                val meanGx = samples.map { it.gravityUnitX }.average().toFloat()
                val meanGy = samples.map { it.gravityUnitY }.average().toFloat()
                val meanGz = samples.map { it.gravityUnitZ }.average().toFloat()
                val maxTiltDeviation = samples.maxOfOrNull {
                    OrientationSensorManager.calculateTiltAngleDeg(
                        meanGx, meanGy, meanGz,
                        it.gravityUnitX, it.gravityUnitY, it.gravityUnitZ
                    )
                } ?: 0f

                // If device was moved during calibration (> 0.45 rad/s or > 6 deg deviation)
                if (maxGyroDuringCal > 0.45f || maxTiltDeviation > 6f) {
                    _movementState.value = MovementState.ERROR
                    triggerHaptic(HapticType.WARNING)
                    onError("Phone was moved during baseline capture. Tap retry while keeping phone still.")
                    return@launch
                }

                // 2. Compute averaged unit gravity vector
                val gMag = sqrt(meanGx * meanGx + meanGy * meanGy + meanGz * meanGz)
                val unitGx = if (gMag > 0.01f) meanGx / gMag else 0f
                val unitGy = if (gMag > 0.01f) meanGy / gMag else 0f
                val unitGz = if (gMag > 0.01f) meanGz / gMag else 1f

                val avgPitch = samples.map { it.pitch }.average().toFloat()
                val avgRoll = samples.map { it.roll }.average().toFloat()

                // Circular mean for azimuth
                var sumSin = 0.0
                var sumCos = 0.0
                for (s in samples) {
                    sumSin += sin(s.azimuth.toDouble())
                    sumCos += cos(s.azimuth.toDouble())
                }
                val avgAzimuth = atan2(sumSin, sumCos).toFloat()

                val visualGrid = cameraManager.visualGridState.value

                // Create stable, immutable reference state
                referenceState = PlacementReferenceState(
                    gravityUnitX = unitGx,
                    gravityUnitY = unitGy,
                    gravityUnitZ = unitGz,
                    pitch = avgPitch,
                    roll = avgRoll,
                    azimuth = avgAzimuth,
                    visualGrid = visualGrid,
                    timestamp = System.currentTimeMillis(),
                    sampleCount = samples.size,
                    isCalibrated = true
                )

                _movementState.value = MovementState.NORMAL
                triggerHaptic(HapticType.SUCCESS)
                onCalibrationComplete()
                startMonitoring()
            } else {
                _movementState.value = MovementState.ERROR
                onError("Failed to obtain sufficient sensor baseline readings.")
            }
        }
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        consecutiveMovedFrames = 0
        consecutiveReturnFrames = 0
        _returnProgress.value = 0f

        monitorJob = coroutineScope.launch(Dispatchers.Default) {
            while (true) {
                delay(monitorLoopIntervalMs)
                val ref = referenceState ?: break
                val currentReading = sensorManager.readingState.value
                val currentVisual = cameraManager.visualGridState.value

                // A. 3D Tilt difference from normalized gravity unit vector
                val tiltDeltaDeg = OrientationSensorManager.calculateTiltAngleDeg(
                    ref.gravityUnitX, ref.gravityUnitY, ref.gravityUnitZ,
                    currentReading.gravityUnitX, currentReading.gravityUnitY, currentReading.gravityUnitZ
                )

                // B. Circular rotation / orientation difference in degrees
                val refReading = ref.toSensorReading()
                val rotationDeltaDeg = OrientationSensorManager.calculateRotationAngleDeg(
                    refReading,
                    currentReading
                )

                // C. Gyroscope angular velocity (motion energy)
                val gyroMag = OrientationSensorManager.getGyroMagnitude(currentReading)
                val isPhoneStill = gyroMag <= maxRestingAngularVelocity

                // D. Camera visual difference (secondary verification layer)
                val visualDiff = PlacementCameraManager.calculateVisualDifference(
                    ref.visualGrid,
                    currentVisual
                )

                // Combined representative delta
                val totalDelta = (tiltDeltaDeg * 0.5f) + (rotationDeltaDeg * 0.3f) + (gyroMag * 2.0f)
                _currentDelta.value = totalDelta

                // Update real-time diagnostics
                _diagnostics.value = PlacementDiagnostics(
                    movementState = _movementState.value,
                    tiltDeltaDeg = tiltDeltaDeg,
                    rotationDeltaDeg = rotationDeltaDeg,
                    gravityDelta = abs(ref.gravityUnitZ - currentReading.gravityUnitZ),
                    angularVelocity = gyroMag,
                    visualDiff = visualDiff,
                    isReferenceCalibrated = ref.isCalibrated,
                    calibrationSampleCount = ref.sampleCount,
                    consecutiveMovedFrames = consecutiveMovedFrames,
                    consecutiveReturnFrames = consecutiveReturnFrames,
                    returnProgress = _returnProgress.value,
                    currentThresholdTilt = sensitivity.moveTiltThresholdDeg,
                    currentReturnTilt = sensitivity.returnTiltThresholdDeg,
                    currentThresholdRotation = sensitivity.moveRotationThresholdDeg,
                    currentReturnRotation = sensitivity.returnRotationThresholdDeg,
                    isStill = isPhoneStill
                )

                // STATE MACHINE EVALUATION
                when (_movementState.value) {
                    MovementState.NORMAL -> {
                        // Check if phone was moved/picked up using MOVE_THRESHOLD
                        val isMovedCondition =
                            (tiltDeltaDeg >= sensitivity.moveTiltThresholdDeg) ||
                            (rotationDeltaDeg >= sensitivity.moveRotationThresholdDeg) ||
                            (gyroMag >= sensitivity.thresholdGyro && tiltDeltaDeg >= (sensitivity.moveTiltThresholdDeg * 0.6f)) ||
                            (visualDiff >= sensitivity.thresholdVisual && visualDiff > 0.05f && gyroMag > 0.3f)

                        if (isMovedCondition) {
                            consecutiveMovedFrames++
                            // Require consecutive frames of confirmed movement to avoid false spikes on light taps
                            if (consecutiveMovedFrames >= moveConfirmationFrames) {
                                consecutiveMovedFrames = 0
                                consecutiveReturnFrames = 0
                                _movementState.value = MovementState.MOVED
                                // Movement penalty event triggered exactly ONCE upon transition
                                _movementEventsCount.value += 1
                                triggerHaptic(HapticType.WARNING)
                            }
                        } else {
                            consecutiveMovedFrames = 0
                        }
                    }

                    MovementState.MOVED -> {
                        // Check if phone was placed back close to ORIGINAL REFERENCE using RETURN_THRESHOLD
                        val isWithinReturnTolerance =
                            (tiltDeltaDeg <= sensitivity.returnTiltThresholdDeg) &&
                            (rotationDeltaDeg <= sensitivity.returnRotationThresholdDeg) &&
                            isPhoneStill && // MUST BE STATIONARY, not moving in someone's hand
                            (visualDiff <= sensitivity.thresholdVisual * 0.85f || visualDiff == 0f)

                        if (isWithinReturnTolerance) {
                            consecutiveReturnFrames = 1
                            _returnProgress.value = (1f / returnConfirmationFrames).coerceIn(0f, 1f)
                            _movementState.value = MovementState.RETURNING
                        }
                    }

                    MovementState.RETURNING -> {
                        // Phone is near original position; verifying stillness and alignment
                        val isStillWithinReturnTolerance =
                            (tiltDeltaDeg <= sensitivity.returnTiltThresholdDeg) &&
                            (rotationDeltaDeg <= sensitivity.returnRotationThresholdDeg) &&
                            isPhoneStill

                        if (isStillWithinReturnTolerance) {
                            consecutiveReturnFrames++
                            _returnProgress.value = (consecutiveReturnFrames.toFloat() / returnConfirmationFrames).coerceIn(0f, 1f)

                            if (consecutiveReturnFrames >= returnConfirmationFrames) {
                                // Full ~1.4s stillness confirmed in original resting setup!
                                consecutiveReturnFrames = 0
                                _returnProgress.value = 1f
                                _movementState.value = MovementState.NORMAL
                                triggerHaptic(HapticType.RESTORED)
                            }
                        } else {
                            // Phone was nudged, picked up, or failed stillness during confirmation!
                            // Immediately abort back to MOVED
                            consecutiveReturnFrames = 0
                            _returnProgress.value = 0f
                            _movementState.value = MovementState.MOVED
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    /**
     * Manual override to restore session when user explicitly taps Resume.
     */
    fun manualResumeFromRestored() {
        if (_movementState.value == MovementState.RETURNING || _movementState.value == MovementState.MOVED) {
            consecutiveMovedFrames = 0
            consecutiveReturnFrames = 0
            _returnProgress.value = 0f
            _movementState.value = MovementState.NORMAL
        }
    }

    fun stopMonitoring() {
        calibrationJob?.cancel()
        monitorJob?.cancel()
        sensorManager.stopListening()
        cameraManager.stopCamera()
        _movementState.value = MovementState.SESSION_ENDED
        _calibrationProgress.value = 0f
        _returnProgress.value = 0f
    }

    fun reset() {
        stopMonitoring()
        referenceState = null
        _movementEventsCount.value = 0
        _movementState.value = MovementState.IDLE
        _currentDelta.value = 0f
        _diagnostics.value = PlacementDiagnostics()
    }

    fun getReferenceState(): PlacementReferenceState? = referenceState

    private fun triggerHaptic(type: HapticType) {
        if (!isVibrationEnabled || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    HapticType.WARNING -> VibrationEffect.createWaveform(longArrayOf(0, 140, 80, 140), -1)
                    HapticType.RESTORED -> VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticType.SUCCESS -> VibrationEffect.createWaveform(longArrayOf(0, 50, 30, 80), -1)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    HapticType.WARNING -> vibrator?.vibrate(longArrayOf(0, 140, 80, 140), -1)
                    HapticType.RESTORED -> vibrator?.vibrate(100)
                    HapticType.SUCCESS -> vibrator?.vibrate(longArrayOf(0, 50, 30, 80), -1)
                }
            }
        } catch (_: Exception) {}
    }

    enum class HapticType {
        WARNING,
        RESTORED,
        SUCCESS
    }
}
