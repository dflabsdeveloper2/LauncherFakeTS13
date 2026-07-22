package com.orbys.launcherts13.domain.usecase

import com.orbys.launcherts13.domain.repository.DesktopRepository
import javax.inject.Inject

class ResizeDesktopItemUseCase @Inject constructor(private val repository: DesktopRepository) {
    operator fun invoke(id: String, colSpan: Int, rowSpan: Int) = repository.resizeItem(id, colSpan, rowSpan)
}
