package com.orbys.launcherfakets13.domain.usecase

import com.orbys.launcherfakets13.domain.repository.WidgetRepository
import javax.inject.Inject

/**
 * Caso de uso para eliminar un widget de la pantalla de inicio.
 */
class RemoveWidgetUseCase @Inject constructor(
    private val repository: WidgetRepository
) {
    /**
     * Elimina permanentemente un widget por su identificador.
     * @param id ID único del AppWidget.
     */
    operator fun invoke(id: Int) = repository.removeWidget(id)
}
