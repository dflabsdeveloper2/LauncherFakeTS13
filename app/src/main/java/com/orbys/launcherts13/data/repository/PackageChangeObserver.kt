package com.orbys.launcherts13.data.repository

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observa altas/bajas/cambios de paquetes vía LauncherApps.Callback, registrado
 * una única vez para toda la vida del proceso (no atado al ciclo de vida de
 * ninguna Activity/Service), a diferencia del BroadcastReceiver dinámico
 * anterior que solo escuchaba mientras MainActivity estaba en foreground.
 */
@Singleton
class PackageChangeObserver @Inject constructor(@ApplicationContext context: Context) {

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = notifyChange()
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = notifyChange()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = notifyChange()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = notifyChange()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = notifyChange()
    }

    init {
        launcherApps.registerCallback(callback)
    }

    private fun notifyChange() {
        _events.tryEmit(Unit)
    }
}
