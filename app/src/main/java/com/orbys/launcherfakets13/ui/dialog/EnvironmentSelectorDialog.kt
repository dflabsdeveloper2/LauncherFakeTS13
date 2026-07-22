package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.domain.model.Environment
import com.orbys.launcherfakets13.ui.util.dp

class EnvironmentSelectorDialog : DialogFragment() {

    var onEnvironmentSelected: ((Environment) -> Unit)? = null
    private var initialEnvironment: Environment? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_environment_selector, null)

        view.findViewById<RecyclerView>(R.id.rvEnvironments).apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = EnvironmentAdapter(Environment.entries, initialEnvironment) { env ->
                onEnvironmentSelected?.invoke(env)
                dismiss()
            }
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
        fun newInstance(current: Environment? = null) = EnvironmentSelectorDialog().apply {
            initialEnvironment = current
        }
    }
}