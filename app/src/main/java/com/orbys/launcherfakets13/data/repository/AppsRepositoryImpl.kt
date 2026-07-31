package com.orbys.launcherfakets13.data.repository

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import com.orbys.launcherfakets13.domain.model.AppInfo
import com.orbys.launcherfakets13.domain.repository.AppsRepository
import javax.inject.Inject

/**
 * Implementación de [AppsRepository] usando LauncherApps, la API oficial
 * para launchers (en vez de PackageManager.queryIntentActivities). Enumera
 * apps de todos los perfiles (principal + trabajo) y devuelve los iconos ya
 * insignia-dos por perfil, para que el resto de capas no necesiten saber de
 * multi-usuario.
 *
 * La enumeración + insignia de iconos es cara (Binder + composición de bitmaps por
 * app), así que el resultado se cachea en un [companion object] compartido por
 * TODAS las instancias del proceso (tanto la inyectada por Hilt como las que
 * AppDrawerController crea a mano), y se invalida solo cuando LauncherApps avisa
 * de un cambio real de paquetes — no en cada apertura del cajón/selector/diálogo.
 */
class AppsRepositoryImpl @Inject constructor(private val context: Context) : AppsRepository {

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    init {
        ensureCacheInvalidationRegistered(context.applicationContext)
    }

    override fun getInstalledApps(): List<AppInfo> {
        cachedApps?.let { return it }
        return synchronized(cacheLock) {
            cachedApps ?: loadInstalledApps().also { cachedApps = it }
        }
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val profiles = runCatching { launcherApps.profiles }
            .getOrDefault(listOf(Process.myUserHandle()))
            .ifEmpty { listOf(Process.myUserHandle()) }

        return profiles
            .flatMap { profile ->
                runCatching { launcherApps.getActivityList(null, profile) }.getOrDefault(emptyList())
            }
            .map { info ->
                AppInfo(
                    label = info.label,
                    packageName = info.applicationInfo.packageName,
                    icon = context.packageManager.getUserBadgedIcon(info.getIcon(0), info.user),
                    componentName = info.componentName,
                    userHandle = info.user
                )
            }
            .sortedBy { it.label.toString().lowercase() }
    }

    companion object {
        @Volatile private var cachedApps: List<AppInfo>? = null
        private val cacheLock = Any()
        @Volatile private var callbackRegistered = false

        private fun ensureCacheInvalidationRegistered(appContext: Context) {
            if (callbackRegistered) return
            synchronized(cacheLock) {
                if (callbackRegistered) return
                callbackRegistered = true
                val launcherApps = appContext.getSystemService(LauncherApps::class.java)
                // registerCallback(callback) crea su Handler con el Looper del hilo que llama —
                // si eso ocurre en un hilo de fondo sin Looper.prepare() (p. ej. el hilo de
                // prefetch()), revienta con "Can't create handler inside thread that has not
                // called Looper.prepare()" y se lleva el proceso entero por delante. Pasando el
                // Handler explícito con el Looper principal, el registro es seguro desde
                // cualquier hilo que llegue a construir la primera instancia de este repositorio.
                launcherApps.registerCallback(
                    object : LauncherApps.Callback() {
                        override fun onPackageAdded(packageName: String?, user: UserHandle?) = invalidate()
                        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = invalidate()
                        override fun onPackageChanged(packageName: String?, user: UserHandle?) = invalidate()
                        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = invalidate()
                        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = invalidate()
                    },
                    Handler(Looper.getMainLooper())
                )
            }
        }

        private fun invalidate() {
            cachedApps = null
        }

        /**
         * Lanza la enumeración en un hilo de fondo si la caché aún está fría, para que
         * esté lista antes de que el usuario llegue a abrir el cajón de apps. Ver
         * LocalDockManager.init.
         */
        fun prefetch(context: Context) {
            if (cachedApps != null) return
            Thread { AppsRepositoryImpl(context.applicationContext).getInstalledApps() }.start()
        }
    }
}
