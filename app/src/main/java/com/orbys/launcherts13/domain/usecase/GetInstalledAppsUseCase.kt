package com.orbys.launcherts13.domain.usecase

import com.orbys.launcherts13.domain.model.AppInfo
import com.orbys.launcherts13.domain.repository.AppsRepository

import javax.inject.Inject

class GetInstalledAppsUseCase @Inject constructor(private val repository: AppsRepository) {
    operator fun invoke(): List<AppInfo> = repository.getInstalledApps()
}
