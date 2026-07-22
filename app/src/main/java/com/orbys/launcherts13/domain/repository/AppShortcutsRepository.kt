package com.orbys.launcherts13.domain.repository

import android.graphics.Rect
import android.os.UserHandle
import com.orbys.launcherts13.domain.model.AppShortcutInfo

/**
 * Interfaz para los shortcuts reales de Android (ShortcutManager) publicados
 * por otras apps: estáticos (manifest), dinámicos y anclados.
 */
interface AppShortcutsRepository {
    fun getShortcutsForApp(packageName: String, userHandle: UserHandle): List<AppShortcutInfo>
    fun startShortcut(shortcut: AppShortcutInfo, sourceBounds: Rect?)
}
