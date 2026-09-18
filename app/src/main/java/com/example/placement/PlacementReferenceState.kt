package com.example.placement

import com.example.domain.model.MovementState
import com.example.sensors.SensorReading
import java.util.Arrays

/**
 * Immutable reference state captured during the calibration window (2-3s).
 *
 * NOTE ON PHYSICAL SENSOR LIMITATIONS:
 * Mobile MEMS IMUs (accelerometer/gyroscope) can measure orientation, gravity vector,
 * and dynamic angular velocity with high precision. However, consumer accelerometers
 * cannot compute absolute centimeter-level physical coordinates on a desk without
 * double-integration drift. Therefore, FocusForest uses sensor fusion (unit gravity vector
 * dot products + circular azimuth/rotation difference + gyroscope angular velocity stillness)
 * as the primary verification layer, supplemented by camera luminance spatial grids when
 * available.
 *
 * Once calibrated, this reference state remains CONSTANT for the entire focus session.
 */
data class PlacementReferenceState(
    val gravityUnitX: Float = 0f,
    val gravityUnitY: Float = 0f,
    val gravityUnitZ: Float = 1f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val azimuth: Float = 0f,
    val visualGrid: FloatArray? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val sampleCount: Int = 0,
    val isCalibrated: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlacementReferenceState) return false
        return gravityUnitX == other.gravityUnitX &&
                gravityUnitY == other.gravityUnitY &&
                gravityUnitZ == other.gravityUnitZ &&
                pitch == other.pitch &&
                roll == other.roll &&
                azimuth == other.azimuth &&
                timestamp == other.timestamp &&
                Arrays.equals(visualGrid, other.visualGrid)
    }

    override fun hashCode(): Int {
        var result = gravityUnitX.hashCode()
        result = 31 * result + gravityUnitY.hashCode()
        result = 31 * result + gravityUnitZ.hashCode()
        result = 31 * result + pitch.hashCode()
        result = 31 * result + roll.hashCode()
        result = 31 * result + azimuth.hashCode()
        result = 31 * result + (visualGrid?.contentHashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        return result
    }

    /**
     * Converts to a legacy SensorReading for backward compatibility where needed.
     */
    fun toSensorReading(): SensorReading {
        return SensorReading(
            accelX = gravityUnitX * 9.80665f,
            accelY = gravityUnitY * 9.80665f,
            accelZ = gravityUnitZ * 9.80665f,
            gravityUnitX = gravityUnitX,
            gravityUnitY = gravityUnitY,
            gravityUnitZ = gravityUnitZ,
            pitch = pitch,
            roll = roll,
            azimuth = azimuth,
            timestamp = timestamp
        )
    }
}

/**
 * Diagnostic metrics exposed for testing, tuning, and Developer Mode.
 */
data class PlacementDiagnostics(
    val movementState: MovementState = MovementState.IDLE,
    val tiltDeltaDeg: Float = 0f,
    val rotationDeltaDeg: Float = 0f,
    val gravityDelta: Float = 0f,
    val angularVelocity: Float = 0f,
    val visualDiff: Float = 0f,
    val isReferenceCalibrated: Boolean = false,
    val calibrationSampleCount: Int = 0,
    val consecutiveMovedFrames: Int = 0,
    val consecutiveReturnFrames: Int = 0,
    val returnProgress: Float = 0f,
    val currentThresholdTilt: Float = 12f,
    val currentReturnTilt: Float = 6.5f,
    val currentThresholdRotation: Float = 16f,
    val currentReturnRotation: Float = 8.5f,
    val isStill: Boolean = true
)
