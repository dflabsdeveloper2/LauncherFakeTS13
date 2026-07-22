package com.orbys.launcherts13.domain.usecase

import com.orbys.launcherts13.domain.repository.DesktopRepository
import javax.inject.Inject

class AddDesktopAppUseCase @Inject constructor(private val repository: DesktopRepository) {
    operator fun invoke(id: String, row: Int, col: Int, packageName: String, label: String) {
        repository.addAppItem(id, row, col, packageName, label)
    }
}
