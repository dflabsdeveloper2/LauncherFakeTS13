package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.R

/**
 * Selector de tipo de elemento a añadir en una celda vacía del Desktop (App o Widget),
 * con el mismo diseño de tarjetas que [WallpaperOptionsDialog].
 */
class DesktopAddItemDialog : DialogFragment() {

    var onAddApp: (() -> Unit)? = null
    var onAddWidget: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_desktop_add_item, null)

        view.findViewById<View>(R.id.btn_add_app).setOnClickListener {
            onAddApp?.invoke()
            dismiss()
        }

        view.findViewById<View>(R.id.btn_add_widget).setOnClickListener {
            onAddWidget?.invoke()
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()
            .also { dialog ->
                dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
            }
    }

    companion object {
        fun newInstance() = DesktopAddItemDialog()
    }
}
