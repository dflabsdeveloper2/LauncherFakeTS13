package com.orbys.launcherfakets13.services

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.orbys.launcherfakets13.domain.usecase.RemapWidgetIdUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Recibe android.appwidget.action.APPWIDGET_HOST_RESTORED cuando un backup
 * restaura widgets en otro dispositivo/instalación y el sistema les asigna
 * nuevos appWidgetId. Sin esto, los widgets guardados quedan huérfanos
 * (removeFailedWidget los descarta al no encontrar su AppWidgetProviderInfo).
 */
@AndroidEntryPoint
class WidgetRestoreReceiver : BroadcastReceiver() {

    @Inject
    lateinit var remapWidgetIdUseCase: RemapWidgetIdUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AppWidgetManager.ACTION_APPWIDGET_HOST_RESTORED) return

        val oldIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_OLD_IDS)
        val newIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
        if (oldIds == null || newIds == null || oldIds.size != newIds.size) return

        oldIds.forEachIndexed { index, oldId ->
            remapWidgetIdUseCase(oldId, newIds[index])
        }
    }
}
