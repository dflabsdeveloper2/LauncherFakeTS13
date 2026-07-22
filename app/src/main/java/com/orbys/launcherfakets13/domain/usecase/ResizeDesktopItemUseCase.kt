package com.orbys.launcherfakets13.domain.usecase

import com.orbys.launcherfakets13.domain.repository.DesktopRepository
import javax.inject.Inject

class ResizeDesktopItemUseCase @Inject constructor(private val repository: DesktopRepository) {
    operator fun invoke(id: String, colSpan: Int, rowSpan: Int) = repository.resizeItem(id, colSpan, rowSpan)
}
