package com.orbys.launcherfakets13.domain.usecase

import com.orbys.launcherfakets13.domain.repository.DesktopRepository
import javax.inject.Inject

class AddDesktopAppUseCase @Inject constructor(private val repository: DesktopRepository) {
    operator fun invoke(id: String, row: Int, col: Int, packageName: String, label: String) {
        repository.addAppItem(id, row, col, packageName, label)
    }
}
