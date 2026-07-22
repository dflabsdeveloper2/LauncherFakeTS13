package com.orbys.launcherts13.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.orbys.launcherts13.domain.model.DesktopItemInfo
import com.orbys.launcherts13.domain.model.DesktopItemType
import com.orbys.launcherts13.domain.repository.DesktopRepository
import javax.inject.Inject

/**
 * Implementación de [DesktopRepository] utilizando SharedPreferences,
 * siguiendo el mismo esquema de claves por campo que [WidgetRepositoryImpl].
 */
class DesktopRepositoryImpl @Inject constructor(context: Context) : DesktopRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("desktop_prefs", Context.MODE_PRIVATE)

    override fun getItems(): List<DesktopItemInfo> {
        val ids = prefs.all.keys
            .filter { it.startsWith("d_") && it.endsWith("_type") }
            .map { it.removePrefix("d_").removeSuffix("_type") }

        return ids.mapNotNull { id ->
            val typeStr = prefs.getString("d_${id}_type", null) ?: return@mapNotNull null
            val type = runCatching { DesktopItemType.valueOf(typeStr) }.getOrNull() ?: return@mapNotNull null
            val row = prefs.getInt("d_${id}_row", -1)
            val col = prefs.getInt("d_${id}_col", -1)
            if (row == -1 || col == -1) return@mapNotNull null
            DesktopItemInfo(
                id = id,
                type = type,
                row = row,
                col = col,
                colSpan = prefs.getInt("d_${id}_colSpan", 1),
                rowSpan = prefs.getInt("d_${id}_rowSpan", 1),
                packageName = prefs.getString("d_${id}_pkg", null),
                label = prefs.getString("d_${id}_label", null),
                appWidgetId = if (type == DesktopItemType.WIDGET) prefs.getInt("d_${id}_wid", -1) else null
            )
        }
    }

    override fun addAppItem(id: String, row: Int, col: Int, packageName: String, label: String) {
        prefs.edit()
            .putString("d_${id}_type", DesktopItemType.APP.name)
            .putInt("d_${id}_row", row)
            .putInt("d_${id}_col", col)
            .putInt("d_${id}_colSpan", 1)
            .putInt("d_${id}_rowSpan", 1)
            .putString("d_${id}_pkg", packageName)
            .putString("d_${id}_label", label)
            .apply()
    }

    override fun addWidgetItem(id: String, row: Int, col: Int, colSpan: Int, rowSpan: Int, appWidgetId: Int) {
        prefs.edit()
            .putString("d_${id}_type", DesktopItemType.WIDGET.name)
            .putInt("d_${id}_row", row)
            .putInt("d_${id}_col", col)
            .putInt("d_${id}_colSpan", colSpan)
            .putInt("d_${id}_rowSpan", rowSpan)
            .putInt("d_${id}_wid", appWidgetId)
            .apply()
    }

    override fun moveItem(id: String, row: Int, col: Int) {
        prefs.edit()
            .putInt("d_${id}_row", row)
            .putInt("d_${id}_col", col)
            .apply()
    }

    override fun resizeItem(id: String, colSpan: Int, rowSpan: Int) {
        prefs.edit()
            .putInt("d_${id}_colSpan", colSpan)
            .putInt("d_${id}_rowSpan", rowSpan)
            .apply()
    }

    override fun removeItem(id: String) {
        prefs.edit()
            .remove("d_${id}_type")
            .remove("d_${id}_row")
            .remove("d_${id}_col")
            .remove("d_${id}_colSpan")
            .remove("d_${id}_rowSpan")
            .remove("d_${id}_pkg")
            .remove("d_${id}_label")
            .remove("d_${id}_wid")
            .apply()
    }

    override fun removeItemByWidgetId(appWidgetId: Int) {
        val target = getItems().firstOrNull { it.type == DesktopItemType.WIDGET && it.appWidgetId == appWidgetId }
            ?: return
        removeItem(target.id)
    }
}
