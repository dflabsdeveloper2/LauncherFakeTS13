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

class WeatherDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_weather, null)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()
            .also { dialog ->
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog.window?.setGravity(Gravity.CENTER)
            }
    }

    override fun onStart() {
        super.onStart()
        // Large dialog width
        dialog?.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    companion object {
        fun newInstance() = WeatherDialog()
    }
}
