package com.orbys.launcherfakets13.ui.common

import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.domain.model.AppShortcutInfo

/**
 * Menú de shortcuts reales de Android (ShortcutManager) de una app, mostrado
 * al mantener pulsado su icono. Reutiliza PopupMenu en vez de un Fragment
 * nuevo, ya que solo hay que listar 1-4 items de texto.
 */
object AppShortcutsMenu {

    fun show(anchor: View, shortcuts: List<AppShortcutInfo>, onShortcutClick: (AppShortcutInfo) -> Unit) {
        if (shortcuts.isEmpty()) {
            Toast.makeText(anchor.context, R.string.no_shortcuts_available, Toast.LENGTH_SHORT).show()
            return
        }

        val popup = PopupMenu(anchor.context, anchor)
        shortcuts.forEachIndexed { index, shortcut ->
            popup.menu.add(0, index, index, shortcut.label)
        }
        popup.setOnMenuItemClickListener { item ->
            onShortcutClick(shortcuts[item.itemId])
            true
        }
        popup.show()
    }
}
