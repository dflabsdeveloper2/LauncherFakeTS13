package com.orbys.launcherts13.domain.model

/**
 * Representa la información de un widget configurado en el launcher.
 */
data class WidgetInfo(
    val appWidgetId: Int,
    val widthDp: Int,
    val heightDp: Int,
    val xDp: Int,
    val yDp: Int
)
