package com.orbys.launcherts13.domain.usecase

import com.orbys.launcherts13.domain.repository.DesktopRepository
import javax.inject.Inject

class RemoveDesktopItemUseCase @Inject constructor(private val repository: DesktopRepository) {
    operator fun invoke(id: String) = repository.removeItem(id)
}
