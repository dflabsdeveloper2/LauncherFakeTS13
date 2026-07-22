package com.orbys.launcherfakets13.domain.model

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle

/**
 * Representa la información básica de una aplicación instalada en el sistema.
 *
 * @property label Nombre visible de la aplicación.
 * @property packageName Nombre del paquete de la aplicación.
 * @property icon Icono de la aplicación.
 * @property componentName Componente de la actividad de lanzamiento (para LauncherApps.startMainActivity).
 * @property userHandle Perfil de usuario al que pertenece la app (perfil principal o de trabajo).
 */
data class AppInfo(
    val label: CharSequence,
    val packageName: CharSequence,
    val icon: Drawable? = null,
    val componentName: ComponentName? = null,
    val userHandle: UserHandle = Process.myUserHandle()
)
