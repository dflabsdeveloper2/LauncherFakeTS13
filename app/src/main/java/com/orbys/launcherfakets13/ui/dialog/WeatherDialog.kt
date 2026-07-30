package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.util.setupDialogSize

class WeatherDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_weather, null)

        view.findViewById<View>(R.id.btn_close_weather).setOnClickListener {
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext(), R.style.CustomDialogTheme)
            .setView(view)
            .create()
            .also { dialog ->
                dialog.window?.setBackgroundDrawableResource(R.drawable.bg_weather_dialog)
                dialog.window?.setGravity(Gravity.CENTER)
            }
    }

    override fun onStart() {
        super.onStart()
        setupDialogSize(R.fraction.dialog_width_large)
    }

    companion object {
        fun newInstance() = WeatherDialog()
    }
}
