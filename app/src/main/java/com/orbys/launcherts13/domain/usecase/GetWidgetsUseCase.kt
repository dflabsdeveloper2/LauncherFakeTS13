package com.orbys.launcherts13.domain.usecase

import com.orbys.launcherts13.domain.model.WidgetInfo
import com.orbys.launcherts13.domain.repository.WidgetRepository
import javax.inject.Inject

/**
 * Caso de uso para obtener la lista de widgets configurados.
 */
class GetWidgetsUseCase @Inject constructor(
    private val repository: WidgetRepository
) {
    /**
     * Recupera todos los widgets almacenados.
     * @return Lista de [WidgetInfo].
     */
    operator fun invoke(): List<WidgetInfo> = repository.getWidgets()
}
