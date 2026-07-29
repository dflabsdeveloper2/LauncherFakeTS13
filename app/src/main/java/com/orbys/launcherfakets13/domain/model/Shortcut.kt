package com.orbys.launcherfakets13.domain.model

/**
 * Representa un acceso directo a una aplicación dentro de una categoría.
 */
data class Shortcut(
    val packageName: String,
    val label: String,
    val labelRes: Int? = null
)
