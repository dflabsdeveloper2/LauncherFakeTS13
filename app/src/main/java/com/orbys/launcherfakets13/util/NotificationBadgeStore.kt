package com.orbys.launcherfakets13.util

import java.util.concurrent.atomic.AtomicReference

/**
 * Recuento de notificaciones activas por paquete (CDD 3.8.1/H-SR-3: badges de notificación
 * en los iconos). Alimentado por LauncherNotificationListenerService y leído por AppAdapter
 * al dibujar cada icono.
 */
object NotificationBadgeStore {
    private val counts = AtomicReference<Map<String, Int>>(emptyMap())

    fun update(newCounts: Map<String, Int>) {
        counts.set(newCounts)
    }

    fun countFor(packageName: String): Int = counts.get()[packageName] ?: 0
}
