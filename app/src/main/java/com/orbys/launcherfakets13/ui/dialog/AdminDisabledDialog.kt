package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.util.dp

class AdminDisabledDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_admin_disabled, null)

        view.findViewById<View>(R.id.btn_accept).setOnClickListener {
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()
            .also { dialog ->
                dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
                dialog.window?.setGravity(Gravity.CENTER)
                dialog.window?.attributes = dialog.window?.attributes?.apply {
                    y -= 30.dp
                }
            }
    }

    companion object {
        fun newInstance() = AdminDisabledDialog()
    }
}
