package com.orbys.launcherts13.util

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.widget.Toast
import com.orbys.launcherts13.R

object AppLauncherHelper {

    private val listPackagesNoSupportMultiWindow = listOf<String>()

    @SuppressLint("BlockedPrivateApi", "PrivateApi")
    fun startAppInFreeform(context: Context, packageName: String) {
        Log.d("SPLITSCREEN", "startActivityInFreeformModeB")

        if (listPackagesNoSupportMultiWindow.contains(packageName)) {
            Toast.makeText(context, R.string.error_no_multiwindow, Toast.LENGTH_SHORT).show()
            return
        }

        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            it.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

            val options = ActivityOptions.makeBasic()

            // Establish window bounds (example values, could be dynamic)
            val width = 1920
            val height = 1080
            val left = 0
            val top = 0
            val right = width
            val bottom = height
            options.setLaunchBounds(Rect(left, top, right, bottom))

            try {
                val windowConfigurationClass = Class.forName("android.app.WindowConfiguration")
                val freeformMode = windowConfigurationClass.getDeclaredField("WINDOWING_MODE_FREEFORM")
                    .getInt(null)

                val setLaunchWindowingModeMethod = ActivityOptions::class.java.getMethod(
                    "setLaunchWindowingMode",
                    Int::class.javaPrimitiveType
                )
                setLaunchWindowingModeMethod.invoke(options, freeformMode)
            } catch (e: Exception) {
                Log.w("SPLITSCREEN", "Failed to set freeform mode via reflection", e)
            }

            try {
                context.startActivity(it, options.toBundle())
            } catch (e: Exception) {
                Log.e("SPLITSCREEN", "Failed to start activity in freeform", e)
                Toast.makeText(context, R.string.error_split_screen, Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(context, R.string.error_app_not_found, Toast.LENGTH_SHORT).show()
        }
    }
}
