package com.orbys.launcherfakets13.domain.usecase

import com.orbys.launcherfakets13.domain.model.DesktopItemInfo
import com.orbys.launcherfakets13.domain.repository.DesktopRepository
import javax.inject.Inject

class GetDesktopItemsUseCase @Inject constructor(private val repository: DesktopRepository) {
    operator fun invoke(): List<DesktopItemInfo> = repository.getItems()
}
