package com.orbys.launcherts13.data.repository

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.UserHandle
import com.orbys.launcherts13.domain.model.AppShortcutInfo
import com.orbys.launcherts13.domain.repository.AppShortcutsRepository
import javax.inject.Inject

/**
 * Implementación de [AppShortcutsRepository] sobre LauncherApps.getShortcuts()/
 * startShortcut(). Estos métodos lanzan SecurityException si esta app no es
 * el launcher por defecto del dispositivo, de ahí el runCatching defensivo.
 */
class AppShortcutsRepositoryImpl @Inject constructor(context: Context) : AppShortcutsRepository {

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    override fun getShortcutsForApp(packageName: String, userHandle: UserHandle): List<AppShortcutInfo> {
        return runCatching {
            val query = LauncherApps.ShortcutQuery()
                .setPackage(packageName)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                )
            launcherApps.getShortcuts(query, userHandle)
                ?.map { shortcut ->
                    AppShortcutInfo(
                        id = shortcut.id,
                        packageName = shortcut.`package`,
                        label = shortcut.shortLabel ?: shortcut.id,
                        icon = launcherApps.getShortcutIconDrawable(shortcut, 0),
                        userHandle = shortcut.userHandle
                    )
                }
                .orEmpty()
        }.getOrDefault(emptyList())
    }

    override fun startShortcut(shortcut: AppShortcutInfo, sourceBounds: Rect?) {
        runCatching {
            launcherApps.startShortcut(shortcut.packageName, shortcut.id, sourceBounds, null, shortcut.userHandle)
        }
    }
}
