package com.orbys.launcherts13.domain.usecase

import com.orbys.launcherts13.domain.model.DesktopItemInfo
import com.orbys.launcherts13.domain.repository.DesktopRepository
import javax.inject.Inject

class GetDesktopItemsUseCase @Inject constructor(private val repository: DesktopRepository) {
    operator fun invoke(): List<DesktopItemInfo> = repository.getItems()
}
