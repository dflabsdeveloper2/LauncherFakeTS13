package com.orbys.launcherts13.domain.repository

import com.orbys.launcherts13.domain.model.WidgetInfo

/**
 * Contrato para la persistencia y gestión de los widgets de la pantalla de inicio.
 */
interface WidgetRepository {
    /** Recupera todos los widgets configurados. */
    fun getWidgets(): List<WidgetInfo>
    
    /** Añade un nuevo widget al sistema. */
    fun addWidget(appWidgetId: Int, xDp: Int, yDp: Int, widthDp: Int, heightDp: Int)
    
    /** Actualiza la posición de un widget existente. */
    fun updatePosition(appWidgetId: Int, xDp: Int, yDp: Int)
    
    /** Actualiza el tamaño de un widget existente. */
    fun updateSize(appWidgetId: Int, widthDp: Int, heightDp: Int)
    
    /** Elimina un widget por su ID. */
    fun removeWidget(appWidgetId: Int)

    /** Traslada la configuración guardada de un widget a un nuevo ID tras un restore de backup. */
    fun remapWidgetId(oldAppWidgetId: Int, newAppWidgetId: Int)
}
