package com.orbys.launcherfakets13.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.domain.model.Shortcut
import com.orbys.launcherfakets13.domain.repository.ShortcutsRepository
import androidx.core.content.edit
import javax.inject.Inject

/**
 * Implementación de [ShortcutsRepository] utilizando SharedPreferences.
 */
class ShortcutsRepositoryImpl @Inject constructor(context: Context) : ShortcutsRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CATS = "categories_list"
    }

    private val defaultCats: String by lazy { context.getString(R.string.default_categories) }

    override fun getCategoryNames(): List<String> {
        val s = prefs.getString(KEY_CATS, defaultCats) ?: defaultCats
        return s.split(",").filter { it.isNotBlank() }
    }

    override fun setCategoryNames(names: List<String>) {
        prefs.edit { putString(KEY_CATS, names.joinToString(",")) }
    }

    override fun addCategory(name: String) {
        val cats = getCategoryNames().toMutableList()
        if (!cats.contains(name)) {
            cats.add(name)
            prefs.edit { putString(KEY_CATS, cats.joinToString(",")) }
        }
    }

    override fun removeCategory(name: String) {
        val cats = getCategoryNames().toMutableList()
        cats.remove(name)
        prefs.edit { putString(KEY_CATS, cats.joinToString(",")) }
    }

    override fun getShortcut(category: String, index: Int): Shortcut? {
        val pkg = prefs.getString("shortcut_${category}_${index}_pkg", null) ?: return null
        val lbl = prefs.getString("shortcut_${category}_${index}_lbl", "") ?: ""
        return Shortcut(pkg, lbl)
    }

    override fun setShortcut(category: String, index: Int, packageName: String, label: String) {
        prefs.edit {
            putString("shortcut_${category}_${index}_pkg", packageName)
                .putString("shortcut_${category}_${index}_lbl", label)
        }
    }

    override fun removeShortcut(category: String, index: Int) {
        prefs.edit {
            remove("shortcut_${category}_${index}_pkg")
                .remove("shortcut_${category}_${index}_lbl")
        }
    }
}
