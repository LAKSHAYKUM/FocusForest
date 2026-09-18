package com.example.timer

import com.example.domain.model.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimerSnapshot(
    val plannedDurationMinutes: Int = 25,
    val remainingSeconds: Long = 25 * 60L,
    val elapsedSeconds: Long = 0L,
    val progress: Float = 0f, // 0.0 to 1.0
    val sessionState: SessionState = SessionState.IDLE,
    val startTimestamp: Long = 0L,
    val expectedEndTimestamp: Long = 0L,
    val totalPausedDurationMs: Long = 0L
)

class TimerEngine(
    private val coroutineScope: CoroutineScope
) {

    private val _snapshot = MutableStateFlow(TimerSnapshot())
    val snapshot: StateFlow<TimerSnapshot> = _snapshot.asStateFlow()

    private var startTimestamp: Long = 0L
    private var plannedDurationMs: Long = 25 * 60 * 1000L
    private var pausedAtTimestamp: Long = 0L
    private var totalPausedDurationMs: Long = 0L
    private var isCompletedFired: Boolean = false

    private var tickerJob: Job? = null
    private var onCompletionCallback: (() -> Unit)? = null

    @Synchronized
    fun configure(durationMinutes: Int) {
        val currentState = _snapshot.value.sessionState
        if (currentState == SessionState.ACTIVE || currentState == SessionState.PAUSED ||
            currentState == SessionState.MOVED_WARNING || currentState == SessionState.RESTORED) {
            return
        }
        val safeMinutes = durationMinutes.coerceIn(1, 180)
        plannedDurationMs = safeMinutes * 60 * 1000L
        val seconds = safeMinutes * 60L
        _snapshot.value = TimerSnapshot(
            plannedDurationMinutes = safeMinutes,
            remainingSeconds = seconds,
            elapsedSeconds = 0L,
            progress = 0f,
            sessionState = SessionState.IDLE
        )
    }

    @Synchronized
    fun start(onComplete: () -> Unit) {
        onCompletionCallback = onComplete
        isCompletedFired = false
        val now = System.currentTimeMillis()
        startTimestamp = now
        totalPausedDurationMs = 0L
        pausedAtTimestamp = 0L

        val plannedMinutes = (plannedDurationMs / 60000L).toInt()
        val expectedEndTimestamp = startTimestamp + plannedDurationMs

        _snapshot.value = TimerSnapshot(
            plannedDurationMinutes = plannedMinutes,
            remainingSeconds = plannedDurationMs / 1000L,
            elapsedSeconds = 0L,
            progress = 0f,
            sessionState = SessionState.ACTIVE,
            startTimestamp = startTimestamp,
            expectedEndTimestamp = expectedEndTimestamp,
            totalPausedDurationMs = 0L
        )

        launchTicker()
    }

    @Synchronized
    fun pause(isMovementWarning: Boolean = false) {
        if (_snapshot.value.sessionState != SessionState.ACTIVE) return
        val now = System.currentTimeMillis()
        pausedAtTimestamp = now
        tickerJob?.cancel()

        // Freeze elapsed & remaining time accurately at pause moment
        recalculate(now)

        _snapshot.value = _snapshot.value.copy(
            sessionState = if (isMovementWarning) SessionState.MOVED_WARNING else SessionState.PAUSED
        )
    }

    @Synchronized
    fun resume() {
        val currentState = _snapshot.value.sessionState
        if (currentState != SessionState.PAUSED && currentState != SessionState.MOVED_WARNING && currentState != SessionState.RESTORED) return

        val now = System.currentTimeMillis()
        if (pausedAtTimestamp > 0L) {
            val pausedTime = now - pausedAtTimestamp
            totalPausedDurationMs += pausedTime
            pausedAtTimestamp = 0L
        }

        recalculate(now)

        _snapshot.value = _snapshot.value.copy(
            sessionState = SessionState.ACTIVE,
            totalPausedDurationMs = totalPausedDurationMs
        )

        launchTicker()
    }

    @Synchronized
    fun markRestored() {
        if (_snapshot.value.sessionState == SessionState.MOVED_WARNING) {
            _snapshot.value = _snapshot.value.copy(sessionState = SessionState.RESTORED)
        }
    }

    @Synchronized
    fun endManually(isInterrupted: Boolean) {
        tickerJob?.cancel()
        val now = if (pausedAtTimestamp > 0L) pausedAtTimestamp else System.currentTimeMillis()
        recalculate(now)
        _snapshot.value = _snapshot.value.copy(
            sessionState = if (isInterrupted) SessionState.INTERRUPTED else SessionState.IDLE
        )
    }

    @Synchronized
    fun reset() {
        tickerJob?.cancel()
        isCompletedFired = false
        val plannedMinutes = (_snapshot.value.plannedDurationMinutes).coerceAtLeast(1)
        plannedDurationMs = plannedMinutes * 60 * 1000L
        pausedAtTimestamp = 0L
        totalPausedDurationMs = 0L
        _snapshot.value = TimerSnapshot(
            plannedDurationMinutes = plannedMinutes,
            remainingSeconds = plannedMinutes * 60L,
            elapsedSeconds = 0L,
            progress = 0f,
            sessionState = SessionState.IDLE
        )
    }

    private fun launchTicker() {
        tickerJob?.cancel()
        tickerJob = coroutineScope.launch(Dispatchers.Default) {
            while (true) {
                delay(250L) // Frequent poll ensures zero UI drift
                val now = System.currentTimeMillis()
                val isDone = synchronized(this@TimerEngine) {
                    recalculate(now)
                }
                if (isDone) {
                    var shouldTrigger = false
                    synchronized(this@TimerEngine) {
                        if (!isCompletedFired && _snapshot.value.sessionState == SessionState.ACTIVE) {
                            isCompletedFired = true
                            shouldTrigger = true
                            _snapshot.value = _snapshot.value.copy(
                                remainingSeconds = 0L,
                                progress = 1.0f,
                                sessionState = SessionState.COMPLETED
                            )
                        }
                    }
                    if (shouldTrigger) {
                        onCompletionCallback?.invoke()
                    }
                    break
                }
            }
        }
    }

    @Synchronized
    private fun recalculate(now: Long): Boolean {
        val currentPause = if (pausedAtTimestamp > 0L) (now - pausedAtTimestamp) else 0L
        val effectiveElapsedMs = (now - startTimestamp - totalPausedDurationMs - currentPause).coerceAtLeast(0L)
        val remainingMs = (plannedDurationMs - effectiveElapsedMs).coerceAtLeast(0L)
        val remainingSec = if (remainingMs <= 0L) 0L else (remainingMs + 999L) / 1000L
        val elapsedSec = effectiveElapsedMs / 1000L
        val progress = if (plannedDurationMs > 0) {
            (effectiveElapsedMs.toFloat() / plannedDurationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

        _snapshot.value = _snapshot.value.copy(
            remainingSeconds = remainingSec,
            elapsedSeconds = elapsedSec,
            progress = progress
        )

        return remainingMs <= 0L
    }

    fun getStartTimestamp(): Long = startTimestamp
    fun getActualElapsedSeconds(): Long = _snapshot.value.elapsedSeconds
}
