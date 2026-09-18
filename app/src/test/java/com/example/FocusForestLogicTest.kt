package com.example

import com.example.domain.model.FocusMode
import com.example.domain.model.MovementSensitivity
import com.example.domain.model.SessionState
import com.example.domain.model.TreeStage
import com.example.sensors.OrientationSensorManager
import com.example.sensors.SensorReading
import com.example.timer.TimerEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FocusForestLogicTest {

    @Test
    fun testTreeStagesFromDuration() {
        assertEquals(TreeStage.SEED, TreeStage.fromDuration(10))
        assertEquals(TreeStage.SPROUT, TreeStage.fromDuration(15))
        assertEquals(TreeStage.YOUNG_TREE, TreeStage.fromDuration(25))
        assertEquals(TreeStage.MATURE_TREE, TreeStage.fromDuration(45))
        assertEquals(TreeStage.LARGE_TREE, TreeStage.fromDuration(60))
        assertEquals(TreeStage.LARGE_TREE, TreeStage.fromDuration(90))
    }

    @Test
    fun testSensorDistanceCalculation() {
        val baseline = SensorReading(
            accelX = 0f,
            accelY = 0f,
            accelZ = 9.8f,
            pitch = 0f,
            roll = 0f
        )
        val identical = SensorReading(
            accelX = 0f,
            accelY = 0f,
            accelZ = 9.8f,
            pitch = 0f,
            roll = 0f
        )
        val moved = SensorReading(
            accelX = 2.5f,
            accelY = 1.8f,
            accelZ = 7.0f,
            pitch = 0.4f,
            roll = 0.3f
        )

        val zeroDistance = OrientationSensorManager.calculateDistance(baseline, identical)
        val significantDistance = OrientationSensorManager.calculateDistance(baseline, moved)

        assertEquals(0f, zeroDistance, 0.001f)
        assertTrue(significantDistance > 3.0f)
    }

    @Test
    fun testGyroMagnitudeCalculation() {
        val still = SensorReading(gyroX = 0f, gyroY = 0f, gyroZ = 0f)
        val moving = SensorReading(gyroX = 1.0f, gyroY = 2.0f, gyroZ = 2.0f)

        assertEquals(0f, OrientationSensorManager.getGyroMagnitude(still), 0.001f)
        assertEquals(3.0f, OrientationSensorManager.getGyroMagnitude(moving), 0.001f)
    }

    @Test
    fun testSensitivityLevels() {
        val low = MovementSensitivity.LOW
        val med = MovementSensitivity.MEDIUM
        val high = MovementSensitivity.HIGH

        assertTrue(low.thresholdAccel > med.thresholdAccel)
        assertTrue(med.thresholdAccel > high.thresholdAccel)

        assertTrue(low.thresholdGyro > med.thresholdGyro)
        assertTrue(med.thresholdGyro > high.thresholdGyro)
    }

    @Test
    fun testTimerEngineConfigurationAndReset() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timer = TimerEngine(testScope)

        timer.configure(45)
        var snapshot = timer.snapshot.value
        assertEquals(45, snapshot.plannedDurationMinutes)
        assertEquals(45 * 60L, snapshot.remainingSeconds)
        assertEquals(SessionState.IDLE, snapshot.sessionState)

        timer.configure(15)
        snapshot = timer.snapshot.value
        assertEquals(15, snapshot.plannedDurationMinutes)
        assertEquals(15 * 60L, snapshot.remainingSeconds)

        timer.reset()
        snapshot = timer.snapshot.value
        assertEquals(SessionState.IDLE, snapshot.sessionState)
    }

    @Test
    fun testTimerEnginePauseAndResumeState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val timer = TimerEngine(testScope)

        timer.configure(25)
        timer.start {}
        assertEquals(SessionState.ACTIVE, timer.snapshot.value.sessionState)

        timer.pause(isMovementWarning = true)
        assertEquals(SessionState.MOVED_WARNING, timer.snapshot.value.sessionState)

        timer.markRestored()
        assertEquals(SessionState.RESTORED, timer.snapshot.value.sessionState)

        timer.resume()
        assertEquals(SessionState.ACTIVE, timer.snapshot.value.sessionState)

        timer.endManually(isInterrupted = true)
        assertEquals(SessionState.INTERRUPTED, timer.snapshot.value.sessionState)
    }

    @Test
    fun testVisualGridDifference() {
        val gridA = FloatArray(16) { 0.5f }
        val gridB = FloatArray(16) { 0.5f }
        val gridC = FloatArray(16) { 0.8f }

        val diffIdentical = com.example.camera.PlacementCameraManager.calculateVisualDifference(gridA, gridB)
        val diffChanged = com.example.camera.PlacementCameraManager.calculateVisualDifference(gridA, gridC)

        assertEquals(0f, diffIdentical, 0.001f)
        assertEquals(0.3f, diffChanged, 0.001f)
    }

    @Test
    fun testHysteresisThresholdDisparity() {
        for (sens in listOf(MovementSensitivity.LOW, MovementSensitivity.MEDIUM, MovementSensitivity.HIGH)) {
            // Move threshold MUST be strictly greater than return threshold to prevent boundary oscillation
            assertTrue(
                "Move tilt threshold (${sens.moveTiltThresholdDeg}) must exceed return tilt threshold (${sens.returnTiltThresholdDeg})",
                sens.moveTiltThresholdDeg > sens.returnTiltThresholdDeg
            )
            assertTrue(
                "Move rotation threshold (${sens.moveRotationThresholdDeg}) must exceed return rotation threshold (${sens.returnRotationThresholdDeg})",
                sens.moveRotationThresholdDeg > sens.returnRotationThresholdDeg
            )
        }
    }

    @Test
    fun testTiltAngleCalculation() {
        // Test flat resting position (gravity along Z axis)
        val flatGx = 0f
        val flatGy = 0f
        val flatGz = 1f

        // Same position should yield 0 tilt angle
        val zeroTilt = OrientationSensorManager.calculateTiltAngleDeg(
            flatGx, flatGy, flatGz,
            flatGx, flatGy, flatGz
        )
        assertEquals(0f, zeroTilt, 0.01f)

        // 30 degree tilt: gravity vector tilted by 30 degrees (cos(30 deg) = 0.866, sin(30 deg) = 0.5)
        val tilted30Gx = 0.5f
        val tilted30Gy = 0f
        val tilted30Gz = 0.866025f

        val angle30 = OrientationSensorManager.calculateTiltAngleDeg(
            flatGx, flatGy, flatGz,
            tilted30Gx, tilted30Gy, tilted30Gz
        )
        assertEquals(30f, angle30, 0.5f)

        // 90 degree tilt (standing upright)
        val uprightGx = 0f
        val uprightGy = 1f
        val uprightGz = 0f

        val angle90 = OrientationSensorManager.calculateTiltAngleDeg(
            flatGx, flatGy, flatGz,
            uprightGx, uprightGy, uprightGz
        )
        assertEquals(90f, angle90, 0.5f)
    }

    @Test
    fun testCircularRotationAngleCalculation() {
        val baseReading = SensorReading(azimuth = 0.1f, pitch = 0f, roll = 0f)
        val sameReading = SensorReading(azimuth = 0.1f, pitch = 0f, roll = 0f)
        val diffDeg = OrientationSensorManager.calculateRotationAngleDeg(baseReading, sameReading)
        assertEquals(0f, diffDeg, 0.1f)

        // Test angular delta wrapping around PI
        val readingA = SensorReading(azimuth = 3.10f, pitch = 0f, roll = 0f)
        val readingB = SensorReading(azimuth = -3.10f, pitch = 0f, roll = 0f)
        val wrapDiff = OrientationSensorManager.calculateRotationAngleDeg(readingA, readingB)
        // Difference should be ~0.083 rad (~4.7 deg), not ~6.2 rad (355 deg)
        assertTrue("Circular difference should handle pi boundary correctly, was: $wrapDiff", wrapDiff < 10f)
    }
}
