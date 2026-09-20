package com.example.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusForestApp
import com.example.domain.model.Achievement
import com.example.domain.model.FocusMode
import com.example.domain.model.FocusSession
import com.example.domain.model.ForestProgress
import com.example.domain.model.MovementSensitivity
import com.example.domain.model.MovementState
import com.example.domain.model.SessionState
import com.example.domain.model.Tree
import com.example.domain.model.TreeStage
import com.example.placement.PlacementDiagnostics
import com.example.service.FocusForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionCompletedResult(
    val session: FocusSession,
    val tree: Tree,
    val earnedXP: Int,
    val treeStage: TreeStage
)

data class MainUiState(
    val selectedDurationMinutes: Int = 25,
    val selectedMode: FocusMode = FocusMode.PLACEMENT,
    val sessionState: SessionState = SessionState.IDLE,
    val movementState: MovementState = MovementState.IDLE,
    val remainingSeconds: Long = 25 * 60L,
    val timerProgress: Float = 0f,
    val calibrationProgress: Float = 0f,
    val returnProgress: Float = 0f,
    val movementEventsCount: Int = 0,
    val showEndConfirmation: Boolean = false,
    val completedResult: SessionCompletedResult? = null,
    val cameraPermissionGranted: Boolean = false,
    val cameraPermissionExplanationNeeded: Boolean = false,
    val activeNavTab: Int = 0, // 0: Forest, 1: Focus, 2: Stats, 3: History, 4: Settings
    val historyFilter: String = "ALL",
    val sensitivity: MovementSensitivity = MovementSensitivity.MEDIUM,
    val autoResume: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val warningSoundEnabled: Boolean = true,
    val completionSoundEnabled: Boolean = true,
    val ambientSoundEnabled: Boolean = false,
    val treeStyle: String = "PINE",
    val reducedMotion: Boolean = false,
    val dayNightMode: String = "SYSTEM",
    val onboardingCompleted: Boolean = true,
    val strictLockEnabled: Boolean = false,
    val isStrictLockActive: Boolean = false,
    val isDebugModeEnabled: Boolean = false,
    val diagnostics: PlacementDiagnostics = PlacementDiagnostics()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FocusForestApp
    private val repository = app.repository
    private val dataStore = app.dataStoreManager
    private val timerEngine = app.timerEngine
    private val movementManager = app.movementManager
    val lockManager = app.lockManager
    val soundManager = app.soundManager
    val authManager = app.authManager
    val adManager = app.adManager

    val authState = authManager.authState
    val adMetrics = adManager.metrics
    val adsEnabled = adManager.adsEnabledFlow
    val personalizedConsent = adManager.personalizedConsentFlow

    private val unlockCodeService: com.example.domain.tree.UnlockCodeService =
        com.example.domain.tree.ProductionUnlockCodeService()

    val ownedTrees: androidx.compose.runtime.State<Set<String>> =
        dataStore.ownedTrees.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            setOf("tree_default")
        ).let { flow ->
            // Expose as State or StateFlow
            flow.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                setOf("tree_default")
            )
        }.let { sf ->
            androidx.compose.runtime.mutableStateOf(setOf("tree_default")).also { mutableState ->
                viewModelScope.launch {
                    sf.collectLatest { mutableState.value = it }
                }
            }
        }

    val activeTreeId: androidx.compose.runtime.State<String> =
        androidx.compose.runtime.mutableStateOf("tree_default").also { mutableState ->
            viewModelScope.launch {
                dataStore.activeTreeId.collectLatest { mutableState.value = it }
            }
        }

    private val _currentSessionTreeId = MutableStateFlow("tree_default")
    val currentSessionTreeId: StateFlow<String> = _currentSessionTreeId.asStateFlow()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val forestProgress: StateFlow<ForestProgress> = repository.getForestProgress()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ForestProgress()
        )

    val allTrees: StateFlow<List<Tree>> = repository.getAllTrees()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val todayStats = repository.getTodayStats()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    val recentStats = repository.getRecentStats()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allSessions: StateFlow<List<FocusSession>> = repository.getAllSessions()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val achievements: StateFlow<List<Achievement>> = repository.getAchievements()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    init {
        // Observe Preferences
        viewModelScope.launch {
            dataStore.defaultDurationMinutes.collectLatest { min ->
                _uiState.value = _uiState.value.copy(selectedDurationMinutes = min)
                if (_uiState.value.sessionState == SessionState.IDLE) {
                    timerEngine.configure(min)
                }
            }
        }

        viewModelScope.launch {
            dataStore.placementDefaultEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(
                    selectedMode = if (enabled) FocusMode.PLACEMENT else FocusMode.STANDARD
                )
            }
        }

        viewModelScope.launch {
            dataStore.movementSensitivity.collectLatest { sensStr ->
                val sens = try { MovementSensitivity.valueOf(sensStr) } catch (_: Exception) { MovementSensitivity.MEDIUM }
                movementManager.sensitivity = sens
                _uiState.value = _uiState.value.copy(sensitivity = sens)
            }
        }

        viewModelScope.launch {
            dataStore.autoResume.collectLatest { auto ->
                _uiState.value = _uiState.value.copy(autoResume = auto)
            }
        }

        viewModelScope.launch {
            dataStore.vibrationEnabled.collectLatest { vib ->
                movementManager.isVibrationEnabled = vib
                _uiState.value = _uiState.value.copy(vibrationEnabled = vib)
            }
        }

        viewModelScope.launch {
            dataStore.warningSoundEnabled.collectLatest { ws ->
                _uiState.value = _uiState.value.copy(warningSoundEnabled = ws)
            }
        }

        viewModelScope.launch {
            dataStore.completionSoundEnabled.collectLatest { cs ->
                _uiState.value = _uiState.value.copy(completionSoundEnabled = cs)
            }
        }

        viewModelScope.launch {
            dataStore.ambientSoundEnabled.collectLatest { amb ->
                _uiState.value = _uiState.value.copy(ambientSoundEnabled = amb)
                if (_uiState.value.sessionState == SessionState.ACTIVE) {
                    soundManager.startAmbientSound(amb)
                } else {
                    soundManager.stopAmbientSound()
                }
            }
        }

        viewModelScope.launch {
            dataStore.dayNightMode.collectLatest { dnm ->
                _uiState.value = _uiState.value.copy(dayNightMode = dnm)
            }
        }

        viewModelScope.launch {
            dataStore.treeStyle.collectLatest { ts ->
                _uiState.value = _uiState.value.copy(treeStyle = ts)
            }
        }

        viewModelScope.launch {
            dataStore.reducedMotion.collectLatest { rm ->
                _uiState.value = _uiState.value.copy(reducedMotion = rm)
            }
        }

        viewModelScope.launch {
            dataStore.onboardingCompleted.collectLatest { completed ->
                _uiState.value = _uiState.value.copy(onboardingCompleted = completed)
            }
        }

        viewModelScope.launch {
            dataStore.strictLockEnabled.collectLatest { enabled ->
                lockManager.setStrictLockPreference(enabled)
                _uiState.value = _uiState.value.copy(strictLockEnabled = enabled)
            }
        }

        viewModelScope.launch {
            dataStore.debugDiagnosticsEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(isDebugModeEnabled = enabled)
            }
        }

        viewModelScope.launch {
            lockManager.lockState.collectLatest { state ->
                _uiState.value = _uiState.value.copy(isStrictLockActive = state.isLockActive)
            }
        }

        // Observe Timer Engine
        viewModelScope.launch {
            timerEngine.snapshot.collectLatest { snapshot ->
                _uiState.value = _uiState.value.copy(
                    remainingSeconds = snapshot.remainingSeconds,
                    timerProgress = snapshot.progress,
                    sessionState = snapshot.sessionState
                )
            }
        }

        // Observe Movement Detection
        viewModelScope.launch {
            movementManager.movementState.collectLatest { moveState ->
                _uiState.value = _uiState.value.copy(movementState = moveState)
                handleMovementStateChange(moveState)
            }
        }

        viewModelScope.launch {
            movementManager.calibrationProgress.collectLatest { prog ->
                _uiState.value = _uiState.value.copy(calibrationProgress = prog)
            }
        }

        viewModelScope.launch {
            movementManager.returnProgress.collectLatest { prog ->
                _uiState.value = _uiState.value.copy(returnProgress = prog)
            }
        }

        viewModelScope.launch {
            movementManager.diagnostics.collectLatest { diag ->
                _uiState.value = _uiState.value.copy(diagnostics = diag)
            }
        }

        viewModelScope.launch {
            movementManager.movementEventsCount.collectLatest { count ->
                _uiState.value = _uiState.value.copy(movementEventsCount = count)
            }
        }

        // Observe Foreground Service remote actions
        viewModelScope.launch {
            FocusForegroundService.serviceActions.collectLatest { action ->
                when (action) {
                    "PAUSE" -> pauseSession()
                    "RESUME" -> resumeSession()
                    "STOP" -> confirmEndSession()
                }
            }
        }
    }

    private fun handleMovementStateChange(moveState: MovementState) {
        val currentSessionState = _uiState.value.sessionState
        if (currentSessionState == SessionState.ACTIVE && moveState == MovementState.MOVED) {
            soundManager.playWarningChime(_uiState.value.warningSoundEnabled)
            timerEngine.pause(isMovementWarning = true)
        } else if ((currentSessionState == SessionState.MOVED_WARNING || currentSessionState == SessionState.RESTORED) && moveState == MovementState.NORMAL) {
            if (_uiState.value.autoResume) {
                resumeSession()
            } else {
                timerEngine.markRestored()
            }
        } else if (currentSessionState == SessionState.MOVED_WARNING && moveState == MovementState.RETURNING) {
            // Keep timer paused but mark position restored so UI reflects returning verification
            timerEngine.markRestored()
        } else if (currentSessionState == SessionState.RESTORED && moveState == MovementState.MOVED) {
            // Moved again during return verification
            soundManager.playWarningChime(_uiState.value.warningSoundEnabled)
            timerEngine.pause(isMovementWarning = true)
        }
    }

    private fun isSessionActive(): Boolean {
        return when (_uiState.value.sessionState) {
            SessionState.ACTIVE,
            SessionState.PAUSED,
            SessionState.CALIBRATING,
            SessionState.MOVED_WARNING,
            SessionState.RESTORED -> true
            SessionState.IDLE,
            SessionState.COMPLETED,
            SessionState.INTERRUPTED -> false
        }
    }

    fun selectDuration(minutes: Int) {
        if (isSessionActive()) return
        val safeMinutes = minutes.coerceIn(1, 720)
        _uiState.value = _uiState.value.copy(selectedDurationMinutes = safeMinutes)
        timerEngine.configure(safeMinutes)
    }

    fun selectMode(mode: FocusMode) {
        if (isSessionActive()) return
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun setActiveTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(activeNavTab = tabIndex)
    }

    fun setHistoryFilter(filter: String) {
        _uiState.value = _uiState.value.copy(historyFilter = filter)
    }

    fun setCameraPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            cameraPermissionGranted = granted,
            cameraPermissionExplanationNeeded = !granted
        )
    }

    fun startPlacementCalibration(onComplete: () -> Unit) {
        // Freeze current active tree for this entire session
        _currentSessionTreeId.value = activeTreeId.value

        _uiState.value = _uiState.value.copy(
            sessionState = SessionState.CALIBRATING,
            selectedMode = FocusMode.PLACEMENT
        )
        movementManager.startCalibration(
            durationMs = 2500L,
            onCalibrationComplete = {
                onComplete()
            },
            onError = { _ ->
                // Do NOT fallback to Standard mode. Keep user in CALIBRATING state
                // so they can read the guidance and tap Retry.
                _uiState.value = _uiState.value.copy(
                    sessionState = SessionState.CALIBRATING,
                    selectedMode = FocusMode.PLACEMENT
                )
            }
        )
    }

    fun startFocusSession() {
        // Freeze current active tree for this entire session
        _currentSessionTreeId.value = activeTreeId.value

        val duration = _uiState.value.selectedDurationMinutes
        val isPlacement = _uiState.value.selectedMode == FocusMode.PLACEMENT

        soundManager.startAmbientSound(_uiState.value.ambientSoundEnabled)

        timerEngine.start {
            onSessionCompleted()
        }

        FocusForegroundService.startService(
            app,
            duration * 60L,
            isPlacement = isPlacement
        )
    }

    fun pauseSession() {
        soundManager.stopAmbientSound()
        timerEngine.pause(isMovementWarning = false)
    }

    fun resumeSession() {
        if (_uiState.value.selectedMode == FocusMode.PLACEMENT) {
            movementManager.manualResumeFromRestored()
        }
        soundManager.startAmbientSound(_uiState.value.ambientSoundEnabled)
        timerEngine.resume()
    }

    fun requestEndSessionConfirmation() {
        _uiState.value = _uiState.value.copy(showEndConfirmation = true)
    }

    fun dismissEndConfirmation() {
        _uiState.value = _uiState.value.copy(showEndConfirmation = false)
    }

    fun cancelPlacementCalibration() {
        movementManager.stopMonitoring()
        timerEngine.reset()
        _uiState.value = _uiState.value.copy(
            sessionState = SessionState.IDLE,
            movementState = MovementState.IDLE,
            calibrationProgress = 0f
        )
    }

    fun retryPlacementCalibration() {
        startPlacementCalibration {
            startFocusSession()
        }
    }

    fun confirmEndSession() {
        _uiState.value = _uiState.value.copy(showEndConfirmation = false)
        val startTime = timerEngine.getStartTimestamp()
        val actualSeconds = timerEngine.getActualElapsedSeconds()
        val plannedMinutes = _uiState.value.selectedDurationMinutes
        val mode = _uiState.value.selectedMode
        val movements = _uiState.value.movementEventsCount

        soundManager.stopAmbientSound()
        timerEngine.endManually(isInterrupted = true)
        movementManager.stopMonitoring()
        FocusForegroundService.stopService(app)

        viewModelScope.launch {
            try {
                repository.recordInterruptedSession(
                    startTime = startTime,
                    endTime = System.currentTimeMillis(),
                    plannedMinutes = plannedMinutes,
                    actualSeconds = actualSeconds,
                    mode = mode,
                    movementEvents = movements,
                    treeSpecies = _currentSessionTreeId.value
                )
            } finally {
                timerEngine.reset()
                movementManager.reset()
            }
        }
    }

    private fun onSessionCompleted() {
        val startTime = timerEngine.getStartTimestamp()
        val actualSeconds = timerEngine.getActualElapsedSeconds()
        val plannedMinutes = _uiState.value.selectedDurationMinutes
        val mode = _uiState.value.selectedMode
        val movements = _uiState.value.movementEventsCount

        soundManager.stopAmbientSound()
        soundManager.playCompletionChime(_uiState.value.completionSoundEnabled)
        movementManager.stopMonitoring()
        FocusForegroundService.stopService(app)

        viewModelScope.launch {
            val (session, tree) = repository.recordCompletedSession(
                startTime = startTime,
                endTime = System.currentTimeMillis(),
                plannedMinutes = plannedMinutes,
                actualSeconds = actualSeconds,
                mode = mode,
                movementEvents = movements,
                treeSpecies = _currentSessionTreeId.value
            )
            _uiState.value = _uiState.value.copy(
                completedResult = SessionCompletedResult(
                    session = session,
                    tree = tree,
                    earnedXP = session.earnedXP,
                    treeStage = TreeStage.fromDuration(plannedMinutes)
                )
            )
        }
    }

    fun dismissCompletedModal() {
        _uiState.value = _uiState.value.copy(completedResult = null)
        timerEngine.reset()
        movementManager.reset()
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            dataStore.setOnboardingCompleted(true)
        }
    }

    fun updateSensitivity(sens: MovementSensitivity) {
        viewModelScope.launch {
            dataStore.setMovementSensitivity(sens.name)
        }
    }

    fun updateAutoResume(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setAutoResume(enabled)
        }
    }

    fun updateVibration(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setVibrationEnabled(enabled)
        }
    }

    fun updateReducedMotion(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setReducedMotion(enabled)
        }
    }

    fun updateDefaultDuration(minutes: Int) {
        viewModelScope.launch {
            dataStore.setDefaultDuration(minutes)
        }
    }

    fun updateDayNightMode(mode: String) {
        viewModelScope.launch {
            dataStore.setDayNightMode(mode)
        }
    }

    fun updateStrictLock(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setStrictLockEnabled(enabled)
        }
    }

    fun updateWarningSound(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setWarningSoundEnabled(enabled)
        }
    }

    fun updateCompletionSound(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setCompletionSoundEnabled(enabled)
        }
    }

    fun updateAmbientSound(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setAmbientSoundEnabled(enabled)
        }
    }

    fun updateTreeStyle(style: String) {
        viewModelScope.launch {
            dataStore.setTreeStyle(style)
        }
    }

    fun updatePlacementDefault(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setPlacementDefault(enabled)
        }
    }

    fun updateDebugMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setDebugDiagnosticsEnabled(enabled)
        }
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            repository.clearAllData()
            timerEngine.reset()
            movementManager.reset()
        }
    }

    fun signInWithGoogle(context: android.content.Context, serverClientId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(context, serverClientId)
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }

    fun signInAnonymously(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = authManager.signInAnonymously()
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }

    fun signInWithEmail(email: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = authManager.signInWithEmail(email, pass)
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = authManager.signUpWithEmail(email, pass)
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }

    fun signOut() {
        authManager.signOut()
    }

    fun clearAuthError() {
        authManager.clearError()
    }

    fun updateAdsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            adManager.setAdsEnabled(enabled)
        }
    }

    fun updatePersonalizedConsent(enabled: Boolean) {
        viewModelScope.launch {
            adManager.setPersonalizedConsent(enabled)
        }
    }

    fun resetAdMetrics() {
        viewModelScope.launch {
            adManager.resetMetrics()
        }
    }

    fun selectActiveTree(treeId: String) {
        viewModelScope.launch {
            dataStore.setActiveTreeId(treeId)
            // Also keep treeStyle string in sync for legacy compatibility
            val styleKey = when (treeId) {
                "tree_love" -> "LOVE"
                "tree_sakura" -> "SAKURA"
                "tree_golden" -> "GOLDEN"
                "tree_autumn" -> "AUTUMN"
                "tree_moonlight" -> "MOONLIGHT"
                "tree_mystic" -> "MYSTIC"
                "tree_blossom" -> "BLOSSOM"
                "tree_spirit" -> "SPIRIT"
                else -> "PINE"
            }
            dataStore.setTreeStyle(styleKey)
        }
    }

    fun redeemUnlockCode(
        rawCode: String,
        onResult: (com.example.domain.tree.CodeRedemptionResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = unlockCodeService.redeemCode(rawCode)
            if (result is com.example.domain.tree.CodeRedemptionResult.Success) {
                // Persist new unlocked tree in local storage
                dataStore.addOwnedTree(result.treeId)
            }
            onResult(result)
        }
    }
}
