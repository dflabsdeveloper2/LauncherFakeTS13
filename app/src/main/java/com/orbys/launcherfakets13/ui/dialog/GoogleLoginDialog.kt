package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.util.setupDialogSize

/**
 * Diálogo de inicio de sesión de Google (Fake).
 *
 * Muestra una pantalla simulada de login según el diseño corporativo.
 */
class GoogleLoginDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_google_login, null)

        view.findViewById<View>(R.id.btn_close_google_login).setOnClickListener { dismiss() }
        
        // El botón SIGUIENTE no tiene funcionalidad (Fake)
        view.findViewById<View>(R.id.btn_google_next).setOnClickListener { 
            // Podría añadirse un feedback visual o simplemente cerrar
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()
            .also { dialog ->
                dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
                dialog.window?.setGravity(Gravity.CENTER)
            }
    }

    override fun onStart() {
        super.onStart()
        setupDialogSize(R.fraction.dialog_width_small_plus, R.fraction.dialog_height_medium)
    }

    companion object {
        fun newInstance() = GoogleLoginDialog()
    }
}
