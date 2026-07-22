package com.orbys.launcherts13.domain.usecase

import com.orbys.launcherts13.domain.repository.DesktopRepository
import javax.inject.Inject

class AddDesktopWidgetUseCase @Inject constructor(private val repository: DesktopRepository) {
    operator fun invoke(id: String, row: Int, col: Int, colSpan: Int, rowSpan: Int, appWidgetId: Int) {
        repository.addWidgetItem(id, row, col, colSpan, rowSpan, appWidgetId)
    }
}
