package com.orbys.launcherfakets13.domain.usecase

import com.orbys.launcherfakets13.domain.repository.ShortcutsRepository

import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(private val repository: ShortcutsRepository) {
    operator fun invoke(): List<String> = repository.getCategoryNames()
}
