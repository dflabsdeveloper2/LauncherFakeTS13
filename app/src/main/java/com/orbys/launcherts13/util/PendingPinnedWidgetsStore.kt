package com.orbys.launcherts13.util

import android.content.Context

/**
 * Cola persistente de appWidgetId aceptados vía AppWidgetManager.requestPinAppWidget() de
 * terceros. PinItemConfirmActivity los acepta y aloja pero no conoce la geometría del grid
 * del Desktop, así que deja el id aquí hasta que DesktopFragment pueda colocarlo en una
 * celda libre.
 */
object PendingPinnedWidgetsStore {
    private const val PREFS = "pending_pinned_widgets"
    private const val KEY_IDS = "ids"

    fun add(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_IDS, emptySet())!!.toMutableSet()
        current.add(appWidgetId.toString())
        prefs.edit().putStringSet(KEY_IDS, current).apply()
    }

    fun consumeAll(context: Context): List<Int> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(KEY_IDS, emptySet())!!.mapNotNull { it.toIntOrNull() }
        if (ids.isNotEmpty()) prefs.edit().remove(KEY_IDS).apply()
        return ids
    }
}