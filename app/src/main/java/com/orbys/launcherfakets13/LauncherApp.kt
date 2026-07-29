package com.orbys.launcherfakets13

import android.app.Application
import com.orbys.launcherfakets13.services.Broadcaster
import com.orbys.launcherfakets13.util.SystemActionHelper
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for Hilt dependency injection.
 */
@HiltAndroidApp
class LauncherApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Antes de que la app se cierre por el crash, intentamos restaurar el estado del sistema
            try {
                // Notificar al launcher principal que esta app se está cerrando
                Broadcaster.sendClose(applicationContext)

                // Desbloquear la expansión de la barra de estado si estaba bloqueada
                SystemActionHelper.setStatusBarLocked(applicationContext, false)
            } catch (e: Exception) {
                // Ignorar errores aquí para no tapar el crash original
            } finally {
                // Pasar el control al manejador por defecto de Android
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
