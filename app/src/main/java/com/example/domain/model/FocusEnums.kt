package com.example.domain.model

enum class FocusMode {
    STANDARD,
    PLACEMENT
}

enum class SessionState {
    IDLE,
    CALIBRATING,
    ACTIVE,
    PAUSED,
    MOVED_WARNING,
    RESTORED,
    COMPLETED,
    INTERRUPTED
}

enum class MovementState {
    IDLE,
    CALIBRATING,
    NORMAL,
    MOVED,
    RETURNING,
    SESSION_ENDED,
    ERROR,

    // Backward compatibility aliases
    @Deprecated("Use NORMAL instead", ReplaceWith("NORMAL"))
    LOCKED,
    @Deprecated("Use RETURNING instead", ReplaceWith("RETURNING"))
    RESTORED,
    @Deprecated("Use SESSION_ENDED instead", ReplaceWith("SESSION_ENDED"))
    STOPPED
}

enum class MovementSensitivity(
    val displayName: String,
    val thresholdAccel: Float,
    val thresholdGyro: Float,
    val thresholdVisual: Float,
    val moveTiltThresholdDeg: Float = 12f,
    val returnTiltThresholdDeg: Float = 6.5f,
    val moveRotationThresholdDeg: Float = 16f,
    val returnRotationThresholdDeg: Float = 8.5f
) {
    LOW(
        displayName = "Low",
        thresholdAccel = 2.2f,
        thresholdGyro = 1.4f,
        thresholdVisual = 0.25f,
        moveTiltThresholdDeg = 18f,
        returnTiltThresholdDeg = 9f,
        moveRotationThresholdDeg = 24f,
        returnRotationThresholdDeg = 12f
    ),
    MEDIUM(
        displayName = "Medium",
        thresholdAccel = 1.4f,
        thresholdGyro = 0.8f,
        thresholdVisual = 0.16f,
        moveTiltThresholdDeg = 12f,
        returnTiltThresholdDeg = 6.5f,
        moveRotationThresholdDeg = 16f,
        returnRotationThresholdDeg = 8.5f
    ),
    HIGH(
        displayName = "High",
        thresholdAccel = 0.8f,
        thresholdGyro = 0.4f,
        thresholdVisual = 0.10f,
        moveTiltThresholdDeg = 7f,
        returnTiltThresholdDeg = 3.5f,
        moveRotationThresholdDeg = 10f,
        returnRotationThresholdDeg = 5f
    )
}

enum class TreeStage(val level: Int, val title: String, val minMinutes: Int, val xpValue: Int) {
    SEED(1, "Seed", 0, 20),
    SPROUT(2, "Sprout", 15, 50),
    YOUNG_TREE(3, "Young Tree", 25, 100),
    MATURE_TREE(4, "Mature Tree", 45, 180),
    LARGE_TREE(5, "Ancient Tree", 60, 300);

    companion object {
        fun fromDuration(minutes: Int): TreeStage {
            return when {
                minutes >= 60 -> LARGE_TREE
                minutes >= 45 -> MATURE_TREE
                minutes >= 25 -> YOUNG_TREE
                minutes >= 15 -> SPROUT
                else -> SEED
            }
        }
    }
}
