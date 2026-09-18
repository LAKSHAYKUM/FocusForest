package com.example.lock

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Standard Android DeviceAdminReceiver for FocusForest.
 *
 * When provisioned as a Device Owner or Kiosk profile controller (e.g. via:
 * `adb shell dpm set-device-owner com.aistudio.focusforest.app/.lock.FocusDeviceAdminReceiver`),
 * this allows FocusForest to legitimately whitelist itself for true, escape-proof
 * Lock Task mode during deep work sessions.
 */
class FocusDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
    }
}
