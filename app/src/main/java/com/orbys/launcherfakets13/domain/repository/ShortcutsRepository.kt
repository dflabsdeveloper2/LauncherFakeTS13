package com.orbys.launcherfakets13.domain.repository

import com.orbys.launcherfakets13.domain.model.Shortcut

/**
 * Interfaz para la gestión de categorías y accesos directos (Shortcuts).
 */
interface ShortcutsRepository {
    fun getCategoryNames(): List<String>
    fun setCategoryNames(names: List<String>)
    fun addCategory(name: String)
    fun removeCategory(name: String)
    fun getShortcut(category: String, index: Int): Shortcut?
    fun setShortcut(category: String, index: Int, packageName: String, label: String)
    fun removeShortcut(category: String, index: Int)
}
