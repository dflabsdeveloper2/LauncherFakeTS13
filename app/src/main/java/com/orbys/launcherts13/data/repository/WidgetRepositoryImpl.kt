package com.orbys.launcherts13.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.orbys.launcherts13.domain.model.WidgetInfo
import com.orbys.launcherts13.domain.repository.WidgetRepository
import javax.inject.Inject

/**
 * Implementación de [WidgetRepository] utilizando SharedPreferences.
 * Almacena de manera persistente el tamaño y posición de cada widget.
 */
class WidgetRepositoryImpl @Inject constructor(context: Context) : WidgetRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("widgets_prefs", Context.MODE_PRIVATE)

    override fun getWidgets(): List<WidgetInfo> {
        val allKeys = prefs.all.keys
        val widgetIds = allKeys.filter { it.startsWith("w_") && it.endsWith("_w") }
            .map { it.removePrefix("w_").removeSuffix("_w").toInt() }
        
        return widgetIds.mapNotNull { id ->
            val w = prefs.getInt("w_${id}_w", -1)
            val h = prefs.getInt("w_${id}_h", -1)
            val x = prefs.getInt("w_${id}_x", 0)
            val y = prefs.getInt("w_${id}_y", 0)
            if (w != -1 && h != -1) {
                WidgetInfo(id, w, h, x, y)
            } else null
        }
    }

    override fun addWidget(appWidgetId: Int, xDp: Int, yDp: Int, widthDp: Int, heightDp: Int) {
        prefs.edit()
            .putInt("w_${appWidgetId}_w", widthDp)
            .putInt("w_${appWidgetId}_h", heightDp)
            .putInt("w_${appWidgetId}_x", xDp)
            .putInt("w_${appWidgetId}_y", yDp)
            .apply()
    }

    override fun updateSize(appWidgetId: Int, widthDp: Int, heightDp: Int) {
        prefs.edit()
            .putInt("w_${appWidgetId}_w", widthDp)
            .putInt("w_${appWidgetId}_h", heightDp)
            .apply()
    }

    override fun updatePosition(appWidgetId: Int, xDp: Int, yDp: Int) {
        prefs.edit()
            .putInt("w_${appWidgetId}_x", xDp)
            .putInt("w_${appWidgetId}_y", yDp)
            .apply()
    }

    override fun removeWidget(appWidgetId: Int) {
        prefs.edit()
            .remove("w_${appWidgetId}_w")
            .remove("w_${appWidgetId}_h")
            .remove("w_${appWidgetId}_x")
            .remove("w_${appWidgetId}_y")
            .apply()
    }

    override fun remapWidgetId(oldAppWidgetId: Int, newAppWidgetId: Int) {
        val w = prefs.getInt("w_${oldAppWidgetId}_w", -1)
        val h = prefs.getInt("w_${oldAppWidgetId}_h", -1)
        if (w == -1 || h == -1) return
        val x = prefs.getInt("w_${oldAppWidgetId}_x", 0)
        val y = prefs.getInt("w_${oldAppWidgetId}_y", 0)
        prefs.edit()
            .putInt("w_${newAppWidgetId}_w", w)
            .putInt("w_${newAppWidgetId}_h", h)
            .putInt("w_${newAppWidgetId}_x", x)
            .putInt("w_${newAppWidgetId}_y", y)
            .remove("w_${oldAppWidgetId}_w")
            .remove("w_${oldAppWidgetId}_h")
            .remove("w_${oldAppWidgetId}_x")
            .remove("w_${oldAppWidgetId}_y")
            .apply()
    }
}
