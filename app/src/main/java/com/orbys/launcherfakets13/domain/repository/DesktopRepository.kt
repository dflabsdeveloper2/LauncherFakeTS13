package com.orbys.launcherfakets13.domain.repository

import com.orbys.launcherfakets13.domain.model.DesktopItemInfo

/**
 * Contrato para la persistencia de los elementos (apps y widgets) de la pantalla Desktop.
 */
interface DesktopRepository {
    /** Recupera todos los elementos colocados en el Desktop. */
    fun getItems(): List<DesktopItemInfo>

    /** Añade un icono de app en la celda indicada. */
    fun addAppItem(id: String, row: Int, col: Int, packageName: String, label: String)

    /** Añade un widget ocupando el rango de celdas indicado. */
    fun addWidgetItem(id: String, row: Int, col: Int, colSpan: Int, rowSpan: Int, appWidgetId: Int)

    /** Mueve un elemento existente a una nueva celda. */
    fun moveItem(id: String, row: Int, col: Int)

    /** Actualiza el tamaño (en celdas) de un widget existente. */
    fun resizeItem(id: String, colSpan: Int, rowSpan: Int)

    /** Elimina un elemento por su id. */
    fun removeItem(id: String)

    /** Elimina el elemento widget asociado a un appWidgetId (limpieza de widgets caídos). */
    fun removeItemByWidgetId(appWidgetId: Int)
}
