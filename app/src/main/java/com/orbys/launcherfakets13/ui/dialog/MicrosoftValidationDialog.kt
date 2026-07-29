package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.util.setupDialogSize

/**
 * Diálogo "¿En qué app quieres validar?" del perfil Microsoft 365.
 *
 * Fake: no hay ningún inicio de sesión real. Tocar VALIDAR o USAR
 * AUTHENTICATOR solo cambia visualmente el estado de cada app a "Validado".
 */
class MicrosoftValidationDialog : DialogFragment() {

    private lateinit var btnOutlook: TextView
    private lateinit var btnOneDrive: TextView
    private lateinit var btnTeams: TextView
    private lateinit var btnWord: TextView
    private lateinit var btnCalendar: TextView

    private lateinit var tvSubOutlook: TextView
    private lateinit var tvSubOneDrive: TextView
    private lateinit var tvSubTeams: TextView
    private lateinit var tvSubWord: TextView
    private lateinit var tvSubCalendar: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_microsoft_validation, null)

        btnOutlook = view.findViewById(R.id.btn_validate_outlook)
        btnOneDrive = view.findViewById(R.id.btn_validate_onedrive)
        btnTeams = view.findViewById(R.id.btn_validate_teams)
        btnWord = view.findViewById(R.id.btn_validate_word)
        btnCalendar = view.findViewById(R.id.btn_validate_calendar)

        tvSubOutlook = view.findViewById(R.id.tv_sub_outlook)
        tvSubOneDrive = view.findViewById(R.id.tv_sub_onedrive)
        tvSubTeams = view.findViewById(R.id.tv_sub_teams)
        tvSubWord = view.findViewById(R.id.tv_sub_word)
        tvSubCalendar = view.findViewById(R.id.tv_sub_calendar)

        btnOutlook.setOnClickListener { validateApp(btnOutlook, tvSubOutlook) }
        btnOneDrive.setOnClickListener { validateApp(btnOneDrive, tvSubOneDrive) }
        btnTeams.setOnClickListener { validateApp(btnTeams, tvSubTeams) }
        btnWord.setOnClickListener { validateApp(btnWord, tvSubWord) }
        btnCalendar.setOnClickListener { validateApp(btnCalendar, tvSubCalendar) }

        view.findViewById<View>(R.id.btn_use_authenticator).setOnClickListener {
            AdminDisabledDialog.newInstance().show(parentFragmentManager, "admin_disabled")
        }

        view.findViewById<View>(R.id.btn_close_ms_validation).setOnClickListener { dismiss() }

        return MaterialAlertDialogBuilder(requireContext(), R.style.CustomDialogTheme)
            .setView(view)
            .create()
            .also { dialog ->
                dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
                dialog.window?.setGravity(Gravity.CENTER)
            }
    }

    override fun onStart() {
        super.onStart()
        setupDialogSize(R.fraction.dialog_width_medium, R.fraction.dialog_height_large)
    }

    private fun validateApp(button: TextView, subtitle: TextView) {
      /*  button.text = "OK"
        button.alpha = 0.5f
        button.isEnabled = false
        subtitle.text = "Validado"
        subtitle.setTextColor(Color.parseColor("#4CAF50"))*/
    }

    private fun validateAll() {
        validateApp(btnOutlook, tvSubOutlook)
        validateApp(btnOneDrive, tvSubOneDrive)
        validateApp(btnTeams, tvSubTeams)
        validateApp(btnWord, tvSubWord)
        validateApp(btnCalendar, tvSubCalendar)
    }

    companion object {
        fun newInstance() = MicrosoftValidationDialog()
    }
}
