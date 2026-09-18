package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

data class SensorReading(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 9.8f,
    val gravityUnitX: Float = 0f,
    val gravityUnitY: Float = 0f,
    val gravityUnitZ: Float = 1f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val pitch: Float = 0f,   // In radians
    val roll: Float = 0f,    // In radians
    val azimuth: Float = 0f, // In radians
    val timestamp: Long = System.currentTimeMillis()
)

class OrientationSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationVector = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _readingState = MutableStateFlow(SensorReading())
    val readingState: StateFlow<SensorReading> = _readingState.asStateFlow()

    private var isListening = false

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var currentAccel = FloatArray(3) { if (it == 2) 9.8f else 0f }
    private var filteredGravity = FloatArray(3) { if (it == 2) 9.8f else 0f }
    private var currentGyro = FloatArray(3)
    private var currentAngles = FloatArray(3)

    // Low-pass filter factor (0.80f balances fast response to deliberate movement with rejection of noise)
    private val alpha = 0.80f

    fun hasRequiredSensors(): Boolean {
        return sensorManager != null && accelerometer != null
    }

    fun hasGyroscope(): Boolean = gyroscope != null
    fun hasRotationVector(): Boolean = rotationVector != null

    @Synchronized
    fun startListening() {
        if (isListening || sensorManager == null) return
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gravitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        rotationVector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        isListening = true
    }

    @Synchronized
    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                currentAccel[0] = event.values[0]
                currentAccel[1] = event.values[1]
                currentAccel[2] = event.values[2]

                // If dedicated gravity sensor is absent, apply low-pass filter on accelerometer
                if (gravitySensor == null) {
                    filteredGravity[0] = alpha * filteredGravity[0] + (1 - alpha) * event.values[0]
                    filteredGravity[1] = alpha * filteredGravity[1] + (1 - alpha) * event.values[1]
                    filteredGravity[2] = alpha * filteredGravity[2] + (1 - alpha) * event.values[2]
                }
            }
            Sensor.TYPE_GRAVITY -> {
                // Hardware gravity sensor is already low-pass filtered by sensor HAL
                filteredGravity[0] = alpha * filteredGravity[0] + (1 - alpha) * event.values[0]
                filteredGravity[1] = alpha * filteredGravity[1] + (1 - alpha) * event.values[1]
                filteredGravity[2] = alpha * filteredGravity[2] + (1 - alpha) * event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                currentGyro[0] = event.values[0]
                currentGyro[1] = event.values[1]
                currentGyro[2] = event.values[2]
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                try {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    currentAngles[0] = orientationAngles[0] // Azimuth
                    currentAngles[1] = orientationAngles[1] // Pitch
                    currentAngles[2] = orientationAngles[2] // Roll
                } catch (_: Exception) {}
            }
        }

        // If rotation vector is missing on device, estimate pitch and roll from gravity
        if (rotationVector == null) {
            val gx = filteredGravity[0]
            val gy = filteredGravity[1]
            val gz = filteredGravity[2]
            currentAngles[1] = atan2(-gx, sqrt(gy * gy + gz * gz)) // Pitch
            currentAngles[2] = atan2(gy, gz) // Roll
        }

        // Calculate normalized unit gravity vector for rotation-independent tilt calculation
        val gMag = sqrt(
            filteredGravity[0] * filteredGravity[0] +
            filteredGravity[1] * filteredGravity[1] +
            filteredGravity[2] * filteredGravity[2]
        )
        val unitGx = if (gMag > 0.01f) filteredGravity[0] / gMag else 0f
        val unitGy = if (gMag > 0.01f) filteredGravity[1] / gMag else 0f
        val unitGz = if (gMag > 0.01f) filteredGravity[2] / gMag else 1f

        _readingState.value = SensorReading(
            accelX = currentAccel[0],
            accelY = currentAccel[1],
            accelZ = currentAccel[2],
            gravityUnitX = unitGx,
            gravityUnitY = unitGy,
            gravityUnitZ = unitGz,
            gyroX = currentGyro[0],
            gyroY = currentGyro[1],
            gyroZ = currentGyro[2],
            azimuth = currentAngles[0],
            pitch = currentAngles[1],
            roll = currentAngles[2],
            timestamp = System.currentTimeMillis()
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    companion object {
        /**
         * Calculates 3D tilt difference in degrees using dot product of unit gravity vectors.
         * Independent of yaw / horizontal table rotation.
         */
        fun calculateTiltAngleDeg(
            unitX1: Float, unitY1: Float, unitZ1: Float,
            unitX2: Float, unitY2: Float, unitZ2: Float
        ): Float {
            val dot = (unitX1 * unitX2 + unitY1 * unitY2 + unitZ1 * unitZ2).coerceIn(-1f, 1f)
            val rad = acos(dot)
            return Math.toDegrees(rad.toDouble()).toFloat()
        }

        fun calculateTiltAngleDeg(r1: SensorReading, r2: SensorReading): Float {
            return calculateTiltAngleDeg(
                r1.gravityUnitX, r1.gravityUnitY, r1.gravityUnitZ,
                r2.gravityUnitX, r2.gravityUnitY, r2.gravityUnitZ
            )
        }

        /**
         * Calculates circular angular rotation difference in degrees across azimuth, pitch, and roll.
         * Detects rotation while resting flat on a table as well as tilts.
         */
        fun calculateRotationAngleDeg(r1: SensorReading, r2: SensorReading): Float {
            val twoPi = (2.0 * Math.PI).toFloat()
            val diffAzimuth = kotlin.math.abs(r1.azimuth - r2.azimuth)
            val circularAzimuth = min(diffAzimuth, twoPi - diffAzimuth)

            val diffPitch = kotlin.math.abs(r1.pitch - r2.pitch)
            val diffRoll = kotlin.math.abs(r1.roll - r2.roll)

            val totalRad = sqrt(circularAzimuth * circularAzimuth + diffPitch * diffPitch + diffRoll * diffRoll)
            return Math.toDegrees(totalRad.toDouble()).toFloat()
        }

        /**
         * Gyroscope angular velocity magnitude in rad/s.
         */
        fun getGyroMagnitude(r: SensorReading): Float {
            return sqrt(r.gyroX * r.gyroX + r.gyroY * r.gyroY + r.gyroZ * r.gyroZ)
        }

        /**
         * Legacy distance metric preserved for compatibility.
         */
        fun calculateDistance(r1: SensorReading, r2: SensorReading): Float {
            val dx = r1.accelX - r2.accelX
            val dy = r1.accelY - r2.accelY
            val dz = r1.accelZ - r2.accelZ
            val accelDiff = sqrt(dx * dx + dy * dy + dz * dz)

            val dpitch = kotlin.math.abs(r1.pitch - r2.pitch)
            val droll = kotlin.math.abs(r1.roll - r2.roll)
            val angleDiff = (dpitch + droll) * 2f

            return accelDiff + angleDiff
        }
    }
}
