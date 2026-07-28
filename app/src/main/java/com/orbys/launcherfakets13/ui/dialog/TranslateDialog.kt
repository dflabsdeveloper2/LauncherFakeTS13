package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.util.setupDialogSize

/**
 * Diálogo "Orbys Translate" (traducción en vivo).
 *
 * Fake: no hay ningún motor de traducción real detrás. Los selectores de
 * idioma sí funcionan (cambian el idioma mostrado) y TRADUCIR busca el texto
 * introducido en un pequeño diccionario de frases de ejemplo ES↔EN; si no lo
 * encuentra, muestra un aviso de que esta demo no lo soporta.
 *
 * Autocontenido: solo depende de res/layout/dialog_translate.xml y de los
 * drawables bg_name_selector_* / bg_timer_chip_unselected / bg_translate_*.
 */
class TranslateDialog : DialogFragment() {

    private val languages = listOf("Español", "Inglés", "Francés", "Alemán", "Italiano", "Portugués")

    private val phrasePairs = listOf(
        "Buenos días, hoy vamos a repasar las fracciones y a resolver un reto en equipo." to
            "Good morning, today we are going to review fractions and solve a team challenge.",
        "Buenos días, vamos a empezar." to "Good morning, let's begin.",
        "Abrid el libro por la página 24." to "Open your book to page 24.",
        "Trabajad en parejas, por favor." to "Work in pairs, please."
    )
    private val esToEn: Map<String, String> = phrasePairs.associate { (es, en) -> normalize(es) to en }
    private val enToEs: Map<String, String> = phrasePairs.associate { (es, en) -> normalize(en) to es }

    private lateinit var tvLangSource: TextView
    private lateinit var tvLangTarget: TextView
    private lateinit var etSource: EditText
    private lateinit var tvTarget: TextView

    private var sourceLang = "Español"
    private var targetLang = "Inglés"

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_translate, null)

        tvLangSource = view.findViewById(R.id.tv_lang_source)
        tvLangTarget = view.findViewById(R.id.tv_lang_target)
        etSource = view.findViewById(R.id.et_translate_source)
        tvTarget = view.findViewById(R.id.tv_translate_target)

        tvLangSource.setOnClickListener { showLanguagePicker(tvLangSource, isSource = true) }
        tvLangTarget.setOnClickListener { showLanguagePicker(tvLangTarget, isSource = false) }
        view.findViewById<View>(R.id.btn_swap_languages).setOnClickListener { swapLanguages() }
        view.findViewById<View>(R.id.btn_start_translate).setOnClickListener { translate() }
        view.findViewById<View>(R.id.btn_close_translate).setOnClickListener { dismiss() }

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
        setupDialogSize(R.fraction.dialog_width_medium, R.fraction.dialog_height_environment)
    }

    private fun showLanguagePicker(anchor: TextView, isSource: Boolean) {
        val popup = PopupMenu(requireContext(), anchor)
        languages.forEach { popup.menu.add(it) }
        popup.setOnMenuItemClickListener { item ->
            val selected = item.title.toString()
            if (isSource) sourceLang = selected else targetLang = selected
            anchor.text = "$selected  ▾"
            true
        }
        popup.show()
    }

    private fun swapLanguages() {
        val newSourceLang = targetLang
        val newTargetLang = sourceLang
        sourceLang = newSourceLang
        targetLang = newTargetLang
        tvLangSource.text = "$sourceLang  ▾"
        tvLangTarget.text = "$targetLang  ▾"

        val sourceText = etSource.text.toString()
        etSource.setText(tvTarget.text.toString())
        tvTarget.text = sourceText
    }

    private fun translate() {
        val normalizedInput = normalize(etSource.text.toString())

        val result = when {
            sourceLang == "Español" && targetLang == "Inglés" -> esToEn[normalizedInput]
            sourceLang == "Inglés" && targetLang == "Español" -> enToEs[normalizedInput]
            else -> null
        }

        tvTarget.text = result
            ?: "(Traducción no disponible en esta demo para $sourceLang → $targetLang)"
    }

    private fun normalize(text: String): String = text.trim().trimEnd('.').lowercase()

    companion object {
        fun newInstance() = TranslateDialog()
    }
}
