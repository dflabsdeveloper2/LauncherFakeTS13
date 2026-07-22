package com.orbys.launcherfakets13.domain.model

/**
 * Tipo de elemento colocado en la pantalla Desktop (grid con snap).
 */
enum class DesktopItemType { APP, WIDGET }

/**
 * Representa un elemento (app o widget) colocado en la pantalla Desktop.
 * La posición y el tamaño se expresan en celdas de grid, no en dp libres.
 */
data class DesktopItemInfo(
    val id: String,
    val type: DesktopItemType,
    val row: Int,
    val col: Int,
    val colSpan: Int = 1,
    val rowSpan: Int = 1,
    val packageName: String? = null,
    val label: String? = null,
    val appWidgetId: Int? = null
)
