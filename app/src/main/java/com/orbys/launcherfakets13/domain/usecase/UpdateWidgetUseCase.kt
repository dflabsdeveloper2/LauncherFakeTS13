package com.orbys.launcherfakets13.domain.usecase

import com.orbys.launcherfakets13.domain.repository.WidgetRepository

import javax.inject.Inject

class UpdateWidgetUseCase @Inject constructor(private val repository: WidgetRepository) {
    fun updatePosition(id: Int, x: Int, y: Int) = repository.updatePosition(id, x, y)
    fun updateSize(id: Int, w: Int, h: Int) = repository.updateSize(id, w, h)
}
