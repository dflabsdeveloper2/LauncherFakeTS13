package com.orbys.launcherfakets13.domain.usecase

import com.orbys.launcherfakets13.domain.model.AppInfo
import com.orbys.launcherfakets13.domain.repository.AppsRepository

import javax.inject.Inject

class GetInstalledAppsUseCase @Inject constructor(private val repository: AppsRepository) {
    operator fun invoke(): List<AppInfo> = repository.getInstalledApps()
}
