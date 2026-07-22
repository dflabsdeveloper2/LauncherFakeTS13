package com.orbys.launcherfakets13.domain.usecase

import com.orbys.launcherfakets13.domain.repository.DesktopRepository
import javax.inject.Inject

class RemoveDesktopWidgetByIdUseCase @Inject constructor(private val repository: DesktopRepository) {
    operator fun invoke(appWidgetId: Int) = repository.removeItemByWidgetId(appWidgetId)
}
