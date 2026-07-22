package com.orbys.launcherts13.domain.usecase

import com.orbys.launcherts13.domain.repository.ShortcutsRepository

import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(private val repository: ShortcutsRepository) {
    operator fun invoke(): List<String> = repository.getCategoryNames()
}
