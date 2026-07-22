package com.orbys.launcherfakets13.domain.usecase

import com.orbys.launcherfakets13.domain.repository.ShortcutsRepository

import javax.inject.Inject

class RemoveCategoryUseCase @Inject constructor(private val repository: ShortcutsRepository) {
    operator fun invoke(name: String) = repository.removeCategory(name)
}
