package com.orbys.launcherfakets13.domain.usecase

import com.orbys.launcherfakets13.domain.repository.WidgetRepository
import javax.inject.Inject

/**
 * Caso de uso para trasladar la configuración de un widget a un nuevo ID
 * tras el remapeo que hace el sistema en un restore de backup.
 */
class RemapWidgetIdUseCase @Inject constructor(
    private val repository: WidgetRepository
) {
    operator fun invoke(oldAppWidgetId: Int, newAppWidgetId: Int) =
        repository.remapWidgetId(oldAppWidgetId, newAppWidgetId)
}
