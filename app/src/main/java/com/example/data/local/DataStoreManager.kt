package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_forest_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val KEY_DEFAULT_DURATION = intPreferencesKey("default_duration_min")
        val KEY_PLACEMENT_DEFAULT = booleanPreferencesKey("placement_default")
        val KEY_MOVEMENT_SENSITIVITY = stringPreferencesKey("movement_sensitivity") // LOW, MEDIUM, HIGH
        val KEY_AUTO_RESUME = booleanPreferencesKey("auto_resume")
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val KEY_WARNING_SOUND_ENABLED = booleanPreferencesKey("warning_sound_enabled")
        val KEY_COMPLETION_SOUND_ENABLED = booleanPreferencesKey("completion_sound_enabled")
        val KEY_AMBIENT_SOUND_ENABLED = booleanPreferencesKey("ambient_sound_enabled")
        val KEY_DAY_NIGHT_MODE = stringPreferencesKey("day_night_mode") // SYSTEM, DAY, NIGHT
        val KEY_TREE_STYLE = stringPreferencesKey("tree_style") // PINE, OAK, CEDAR
        val KEY_REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_STRICT_LOCK_ENABLED = booleanPreferencesKey("strict_lock_enabled")
        val KEY_DEBUG_DIAGNOSTICS_ENABLED = booleanPreferencesKey("debug_diagnostics_enabled")
    }

    val defaultDurationMinutes: Flow<Int> = context.dataStore.data.map { it[KEY_DEFAULT_DURATION] ?: 25 }
    val placementDefaultEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_PLACEMENT_DEFAULT] ?: true }
    val movementSensitivity: Flow<String> = context.dataStore.data.map { it[KEY_MOVEMENT_SENSITIVITY] ?: "MEDIUM" }
    val autoResume: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_RESUME] ?: true }
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_VIBRATION_ENABLED] ?: true }
    val warningSoundEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_WARNING_SOUND_ENABLED] ?: true }
    val completionSoundEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_COMPLETION_SOUND_ENABLED] ?: true }
    val ambientSoundEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_AMBIENT_SOUND_ENABLED] ?: false }
    val dayNightMode: Flow<String> = context.dataStore.data.map { it[KEY_DAY_NIGHT_MODE] ?: "SYSTEM" }
    val treeStyle: Flow<String> = context.dataStore.data.map { it[KEY_TREE_STYLE] ?: "PINE" }
    val reducedMotion: Flow<Boolean> = context.dataStore.data.map { it[KEY_REDUCED_MOTION] ?: false }
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_COMPLETED] ?: false }
    val strictLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_STRICT_LOCK_ENABLED] ?: false }
    val debugDiagnosticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_DEBUG_DIAGNOSTICS_ENABLED] ?: false }

    suspend fun setDefaultDuration(minutes: Int) {
        context.dataStore.edit { it[KEY_DEFAULT_DURATION] = minutes }
    }

    suspend fun setPlacementDefault(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PLACEMENT_DEFAULT] = enabled }
    }

    suspend fun setMovementSensitivity(sensitivity: String) {
        context.dataStore.edit { it[KEY_MOVEMENT_SENSITIVITY] = sensitivity }
    }

    suspend fun setAutoResume(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_RESUME] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATION_ENABLED] = enabled }
    }

    suspend fun setWarningSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WARNING_SOUND_ENABLED] = enabled }
    }

    suspend fun setCompletionSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_COMPLETION_SOUND_ENABLED] = enabled }
    }

    suspend fun setAmbientSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AMBIENT_SOUND_ENABLED] = enabled }
    }

    suspend fun setDayNightMode(mode: String) {
        context.dataStore.edit { it[KEY_DAY_NIGHT_MODE] = mode }
    }

    suspend fun setTreeStyle(style: String) {
        context.dataStore.edit { it[KEY_TREE_STYLE] = style }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { it[KEY_REDUCED_MOTION] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setStrictLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_STRICT_LOCK_ENABLED] = enabled }
    }

    suspend fun setDebugDiagnosticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DEBUG_DIAGNOSTICS_ENABLED] = enabled }
    }
}
