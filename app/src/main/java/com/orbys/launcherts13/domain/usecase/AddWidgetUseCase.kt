package com.orbys.launcherts13.domain.usecase

import com.orbys.launcherts13.domain.repository.WidgetRepository
import javax.inject.Inject

/**
 * Caso de uso para añadir un nuevo widget a la pantalla de inicio.
 *
 * @property repository Repositorio de widgets [WidgetRepository].
 */
class AddWidgetUseCase @Inject constructor(
    private val repository: WidgetRepository
) {
    /**
     * Registra un nuevo widget con sus dimensiones iniciales.
     * La posición por defecto suele ser calculada en la capa de UI o ViewModel.
     *
     * @param id ID único del AppWidget asignado por el sistema.
     * @param xDp Posición X en DPs.
     * @param yDp Posición Y en DPs.
     * @param w Ancho en DPs.
     * @param h Alto en DPs.
     */
    operator fun invoke(id: Int, xDp: Int, yDp: Int, w: Int, h: Int) {
        repository.addWidget(id, xDp, yDp, w, h)
    }
}
