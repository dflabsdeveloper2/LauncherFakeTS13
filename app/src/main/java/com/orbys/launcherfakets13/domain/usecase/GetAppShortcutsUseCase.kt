package com.orbys.launcherfakets13.domain.usecase

import android.os.UserHandle
import com.orbys.launcherfakets13.domain.model.AppShortcutInfo
import com.orbys.launcherfakets13.domain.repository.AppShortcutsRepository

import javax.inject.Inject

class GetAppShortcutsUseCase @Inject constructor(private val repository: AppShortcutsRepository) {
    operator fun invoke(packageName: String, userHandle: UserHandle): List<AppShortcutInfo> =
        repository.getShortcutsForApp(packageName, userHandle)
}
