package com.orbys.launcherts13.domain.usecase

import com.orbys.launcherts13.domain.model.Shortcut
import com.orbys.launcherts13.domain.repository.ShortcutsRepository

import javax.inject.Inject

class GetShortcutUseCase @Inject constructor(private val repository: ShortcutsRepository) {
    operator fun invoke(category: String, index: Int): Shortcut? =
        repository.getShortcut(category, index)
}
