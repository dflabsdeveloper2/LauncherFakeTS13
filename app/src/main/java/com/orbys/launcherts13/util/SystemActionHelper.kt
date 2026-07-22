package com.orbys.launcherts13.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.InputEvent
import android.view.KeyEvent
import java.lang.reflect.Method

/**
 * Helper class to perform system actions using privileged APIs.
 * Requires the app to be signed with the platform certificate.
 */
object SystemActionHelper {

    // Mirrors the hidden InputManager.INJECT_INPUT_EVENT_MODE_ASYNC constant (value stable across AOSP).
    private const val INJECT_INPUT_EVENT_MODE_ASYNC = 0

    /**
     * Simulates a Back button press by injecting a KEYCODE_BACK key event via
     * InputManager.injectInputEvent(), a hidden method reachable through reflection
     * because the app holds android.permission.INJECT_EVENTS (signature|privileged).
     * Avoids requiring the user to manually enable an AccessibilityService, which
     * isn't viable on a kiosk/EDLA deployment.
     */
    @SuppressLint("BlockedPrivateApi", "PrivateApi")
    fun performBack(context: Context) {
        try {
            val inputManager = context.getSystemService(Context.INPUT_SERVICE)
            val injectMethod: Method = inputManager.javaClass.getMethod(
                "injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType
            )
            val now = SystemClock.uptimeMillis()
            val downEvent = KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0)
            val upEvent = KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0)
            injectMethod.invoke(inputManager, downEvent, INJECT_INPUT_EVENT_MODE_ASYNC)
            injectMethod.invoke(inputManager, upEvent, INJECT_INPUT_EVENT_MODE_ASYNC)
        } catch (e: Exception) {
            Log.e("SystemActionHelper", "Failed to inject BACK key event", e)
        }
    }

    /**
     * Expands the Notifications panel.
     * Requires android.permission.EXPAND_STATUS_BAR.
     */
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
            val expandMethod: Method = statusBarManagerClass.getMethod("expandNotificationsPanel")
            expandMethod.invoke(statusBarService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Expands the Quick Settings panel.
     * Requires android.permission.EXPAND_STATUS_BAR.
     */
    @SuppressLint("WrongConstant")
    fun expandSettings(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
            val expandMethod: Method = statusBarManagerClass.getMethod("expandSettingsPanel")
            expandMethod.invoke(statusBarService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Locks or unlocks the status bar.
     * Requires android.permission.STATUS_BAR.
     */
    @SuppressLint("WrongConstant")
    fun setStatusBarLocked(context: Context, locked: Boolean) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
            val disableMethod: Method = statusBarManagerClass.getMethod("disable", Int::class.javaPrimitiveType)
            
            // Flags from StatusBarManager:
            // DISABLE_EXPAND = 0x00010000
            // DISABLE_NONE = 0x00000000
            val flags = if (locked) 0x00010000 else 0x00000000
            disableMethod.invoke(statusBarService, flags)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
