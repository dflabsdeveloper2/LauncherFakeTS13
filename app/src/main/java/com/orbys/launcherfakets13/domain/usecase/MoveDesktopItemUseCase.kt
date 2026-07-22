package com.orbys.launcherfakets13.domain.usecase

import com.orbys.launcherfakets13.domain.repository.DesktopRepository
import javax.inject.Inject

class MoveDesktopItemUseCase @Inject constructor(private val repository: DesktopRepository) {
    operator fun invoke(id: String, row: Int, col: Int) = repository.moveItem(id, row, col)
}
