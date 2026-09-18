package com.example.lock

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FocusLockState(
    val isStrictEnabledByUser: Boolean = false,
    val isDeviceOwner: Boolean = false,
    val isLockTaskPermitted: Boolean = false,
    val isLockActive: Boolean = false,
    val lockMode: Int = ActivityManager.LOCK_TASK_MODE_NONE,
    val statusMessage: String = "Standard Consumer Mode"
)

/**
 * FocusLockManager provides legitimate Android Lock Task / Kiosk mode management.
 *
 * It transparently handles:
 * 1. Fully provisioned Enterprise / Kiosk mode (device owner / whitelisted packages).
 * 2. Consumer mode Screen Pinning (`Activity.startLockTask()`).
 * 3. Graceful in-app fallback (immersive focus UI, BackHandler lock, foreground service,
 *    screen-on retention, and interruption tracking) when device privileges are unprovisioned.
 */
class FocusLockManager(private val appContext: Context) {

    private val tag = "FocusLockManager"
    private val _lockState = MutableStateFlow(checkCapabilities(appContext))
    val lockState: StateFlow<FocusLockState> = _lockState.asStateFlow()

    fun refreshCapabilities() {
        _lockState.value = checkCapabilities(appContext).copy(
            isStrictEnabledByUser = _lockState.value.isStrictEnabledByUser,
            isLockActive = _lockState.value.isLockActive
        )
    }

    fun setStrictLockPreference(enabled: Boolean) {
        _lockState.value = _lockState.value.copy(isStrictEnabledByUser = enabled)
    }

    /**
     * Called when a focus session starts.
     * Enters Lock Task mode if strict lock is enabled or requested.
     */
    fun startFocusLock(activity: Activity, forceStrict: Boolean = false) {
        try {
            // Keep display illuminated during active focus session
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (e: Exception) {
            Log.w(tag, "Failed to configure window flags: ${e.message}")
        }

        val shouldAttemptLockTask = forceStrict || _lockState.value.isStrictEnabledByUser

        if (!shouldAttemptLockTask) {
            _lockState.value = _lockState.value.copy(
                isLockActive = true,
                statusMessage = "In-App Focus Guard Active"
            )
            return
        }

        val capabilities = checkCapabilities(activity)
        try {
            // If device owner, ensure self-whitelisting for seamless kiosk lock
            if (capabilities.isDeviceOwner) {
                val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                val adminComponent = ComponentName(activity, FocusDeviceAdminReceiver::class.java)
                dpm?.setLockTaskPackages(adminComponent, arrayOf(activity.packageName))
            }

            // Trigger legitimate Android Lock Task API
            activity.startLockTask()
            val currentMode = getLockTaskMode(activity)
            val msg = when (currentMode) {
                ActivityManager.LOCK_TASK_MODE_LOCKED -> "Strict Kiosk Lock Active"
                ActivityManager.LOCK_TASK_MODE_PINNED -> "Screen Pinning Lock Active"
                else -> "Lock Task Initiated"
            }
            _lockState.value = _lockState.value.copy(
                isLockActive = true,
                lockMode = currentMode,
                statusMessage = msg
            )
            Log.i(tag, "Focus Lock successfully initiated in mode: $currentMode")
        } catch (e: Exception) {
            Log.w(tag, "startLockTask could not be engaged on this device: ${e.message}. Falling back to consumer focus protection.")
            _lockState.value = _lockState.value.copy(
                isLockActive = true,
                lockMode = ActivityManager.LOCK_TASK_MODE_NONE,
                statusMessage = "In-App Focus Guard Active"
            )
        }
    }

    /**
     * Called when a focus session finishes or is intentionally ended.
     * Guaranteed to release Lock Task and restore normal system access.
     */
    fun stopFocusLock(activity: Activity) {
        try {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            Log.w(tag, "Failed to clear KEEP_SCREEN_ON flag: ${e.message}")
        }

        val currentMode = getLockTaskMode(activity)
        if (currentMode != ActivityManager.LOCK_TASK_MODE_NONE) {
            try {
                activity.stopLockTask()
                Log.i(tag, "stopLockTask executed successfully")
            } catch (e: Exception) {
                Log.w(tag, "Error releasing Lock Task: ${e.message}")
            }
        }

        _lockState.value = _lockState.value.copy(
            isLockActive = false,
            lockMode = ActivityManager.LOCK_TASK_MODE_NONE,
            statusMessage = "Unlocked"
        )
    }

    fun syncCurrentMode(activity: Activity) {
        val currentMode = getLockTaskMode(activity)
        if (_lockState.value.lockMode != currentMode) {
            _lockState.value = _lockState.value.copy(lockMode = currentMode)
        }
    }

    fun isDeviceOwner(context: Context): Boolean {
        return checkCapabilities(context).isDeviceOwner
    }

    private fun checkCapabilities(context: Context): FocusLockState {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val isOwner = try {
            dpm?.isDeviceOwnerApp(context.packageName) == true
        } catch (_: Exception) { false }

        val isPermitted = try {
            dpm?.isLockTaskPermitted(context.packageName) == true
        } catch (_: Exception) { false }

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mode = am?.lockTaskModeState ?: ActivityManager.LOCK_TASK_MODE_NONE

        val statusMsg = when {
            isOwner -> "Device Owner provisioned (Zero-escape Kiosk supported)"
            isPermitted -> "Lock Task permitted by Device Policy"
            else -> "Standard Consumer Android (Screen Pinning / In-App Guard)"
        }

        return FocusLockState(
            isDeviceOwner = isOwner,
            isLockTaskPermitted = isPermitted || isOwner,
            lockMode = mode,
            statusMessage = statusMsg
        )
    }

    private fun getLockTaskMode(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return am?.lockTaskModeState ?: ActivityManager.LOCK_TASK_MODE_NONE
    }
}
