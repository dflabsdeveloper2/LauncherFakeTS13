package com.orbys.launcherfakets13.domain.usecase

import android.graphics.Rect
import com.orbys.launcherfakets13.domain.model.AppShortcutInfo
import com.orbys.launcherfakets13.domain.repository.AppShortcutsRepository

import javax.inject.Inject

class LaunchAppShortcutUseCase @Inject constructor(private val repository: AppShortcutsRepository) {
    operator fun invoke(shortcut: AppShortcutInfo, sourceBounds: Rect? = null) =
        repository.startShortcut(shortcut, sourceBounds)
}
