package com.orbys.launcherfakets13.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.orbys.launcherfakets13.util.NotificationBadgeStore

/**
 * Alimenta [NotificationBadgeStore] con el recuento de notificaciones activas por paquete,
 * para pintar badges en los iconos del cajón de apps (CDD 3.8.1/H-SR-3). Requiere que el
 * usuario conceda el acceso a notificaciones desde Ajustes (no es un permiso normal ni se
 * puede pre-conceder vía privapp-permissions.xml).
 */
class LauncherNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshCounts()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        refreshCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        refreshCounts()
    }

    private fun refreshCounts() {

        val counts = runCatching { activeNotifications }.getOrNull()
            ?.filter { it.isCountable() }
            ?.groupingBy { it.packageName }
            ?.eachCount()
            ?: emptyMap()
        NotificationBadgeStore.update(counts)
    }

    // Group summaries would double-count alongside their children; ongoing notifications
    // (foreground services, media controls) aren't the kind of "unread item" a badge implies.
    private fun StatusBarNotification.isCountable(): Boolean {
        val flags = notification.flags
        return flags and Notification.FLAG_GROUP_SUMMARY == 0 &&
            flags and Notification.FLAG_ONGOING_EVENT == 0
    }
}
