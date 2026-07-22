package com.orbys.launcherfakets13.domain.model

import android.graphics.drawable.Drawable
import android.os.UserHandle

/**
 * Shortcut real de Android publicado por otra app (ShortcutManager: estático,
 * dinámico o anclado), distinto de [Shortcut] que es solo el anclaje de una
 * app a un slot de la rejilla de este launcher.
 */
data class AppShortcutInfo(
    val id: String,
    val packageName: String,
    val label: CharSequence,
    val icon: Drawable?,
    val userHandle: UserHandle
)
