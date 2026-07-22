package com.orbys.launcherts13.util

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.RECENT_IGNORE_UNAVAILABLE
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import com.orbys.launcherts13.domain.model.RecentAppInfo
import java.io.File
import java.io.FileOutputStream

/**
 * Utility class for managing recent applications.
 */
object RecentsHelper {

    /**
     * List of packages to exclude from the recent apps view.
     */
    val listPackagesInstalledNoVisibleInRecentApps = listOf(
        "com.orbys.launcherts13",
        "com.android.systemui",
        "com.google.android.inputmethod.latin"
    )

    /**
     * Fetches a list of recent applications.
     */
    fun getRecentBackgroundApps(context: Context): List<RecentAppInfo>? {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val maxRecentApps = 100

        try {
            val recentTasks = activityManager.getRecentTasks(
                maxRecentApps,
                RECENT_IGNORE_UNAVAILABLE
            )

            val recentApps = mutableListOf<RecentAppInfo>()

            for (element in recentTasks) {
                val baseIntent = element.baseIntent

                val packageName = baseIntent.component?.packageName
                if (packageName != null && 
                    recentApps.none { it.packageName == packageName } && 
                    !listPackagesInstalledNoVisibleInRecentApps.contains(packageName)
                ) {

                    val drawable = try {
                        context.packageManager.getApplicationIcon(packageName)
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    }
                    
                    if (drawable != null) {
                        val appIconPath = saveDrawableToInternalStorage(drawable, packageName, context)

                        recentApps.add(
                            RecentAppInfo(
                                packageName = packageName,
                                imagePath = appIconPath,
                                name = getNameLabelApp(context, packageName).orEmpty(),
                                taskId = element.taskId
                            )
                        )
                    }
                }
            }

            return recentApps
        } catch (e: Exception) {
            Log.d("KILLPID", "exception :$e")
            return null
        }
    }

    /**
     * Removes a task from the recent apps list.
     */
    @SuppressLint("BlockedPrivateApi", "PrivateApi")
    fun killApk(taskId: Int, finally: (Boolean) -> Unit) {
        try {
            val activityTaskManagerClass = Class.forName("android.app.ActivityTaskManager")

            // Obtiene el método estático getService() de la clase externa
            val getServiceMethod = activityTaskManagerClass.getDeclaredMethod("getService")
            getServiceMethod.isAccessible = true

            // Llama al método estático getService() para obtener una instancia del servicio
            val serviceInstance = getServiceMethod.invoke(null)

            val iActivityTaskManagerClass = Class.forName("android.app.IActivityTaskManager")

            // Obtiene la clase del parámetro del método removeTask()
            val taskIdClass = Int::class.javaPrimitiveType

            // Obtiene el método removeTask() de la clase ActivityTaskManager
            val removeTaskMethod =
                iActivityTaskManagerClass.getDeclaredMethod("removeTask", taskIdClass)
            removeTaskMethod.isAccessible = true

            // Llama al método removeTask() pasando el ID de la tarea
            val a = removeTaskMethod.invoke(serviceInstance, taskId) as Boolean
            Log.d("KILLPID", "invoke metodo result: $a")
            finally(a)
        } catch (e: Exception) {
            Log.d("KILLPID", "Failed to remove exception $e")
            finally(false)
        }
    }

    /**
     * Saves a [Drawable] as a PNG file in internal storage and returns the absolute path.
     */
    private fun saveDrawableToInternalStorage(drawable: Drawable, packageName: String, context: Context): String {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        val directory = File(context.cacheDir, "app_icons")
        if (!directory.exists()) directory.mkdirs()

        val file = File(directory, "$packageName.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return file.absolutePath
    }

    /**
     * Gets the application label for a given package name.
     */
    private fun getNameLabelApp(context: Context, packageName: String): String? {
        return try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
