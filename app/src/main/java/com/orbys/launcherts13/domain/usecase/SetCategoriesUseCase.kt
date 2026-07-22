package com.orbys.launcherts13.domain.usecase

import com.orbys.launcherts13.domain.repository.ShortcutsRepository
import javax.inject.Inject

class SetCategoriesUseCase @Inject constructor(private val repository: ShortcutsRepository) {
    operator fun invoke(names: List<String>) = repository.setCategoryNames(names)
}
