package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.DialogFragment
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.util.setupDialogSize

class WallpaperOptionsDialog : DialogFragment() {

    var onChangeWallpaper: (() -> Unit)? = null
    var onRemoveWallpaper: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_wallpaper_options, null)

        view.findViewById<View>(R.id.btn_change_wallpaper).setOnClickListener {
            onChangeWallpaper?.invoke()
            dismiss()
        }

        view.findViewById<View>(R.id.btn_remove_wallpaper).setOnClickListener {
            onRemoveWallpaper?.invoke()
            dismiss()
        }

        view.findViewById<View>(R.id.btn_close_wallpaper).setOnClickListener { dismiss() }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()
            .also { dialog ->
                dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
            }
    }

    override fun onStart() {
        super.onStart()
        setupDialogSize(R.fraction.dialog_width_small)
    }

    companion object {
        fun newInstance() = WallpaperOptionsDialog()
    }
}