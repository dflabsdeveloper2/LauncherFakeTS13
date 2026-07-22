package com.orbys.launcherts13.domain.usecase

import com.orbys.launcherts13.domain.repository.ShortcutsRepository

import javax.inject.Inject

class SetShortcutUseCase @Inject constructor(private val repository: ShortcutsRepository) {
    operator fun invoke(category: String, index: Int, packageName: String, label: String) {
        repository.setShortcut(category, index, packageName, label)
    }
}
