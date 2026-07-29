package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.util.setupDialogSize
import kotlin.random.Random

/**
 * Diálogo "ruleta" que elige al azar un nombre de una lista predefinida.
 */
class NameSelectorDialog : DialogFragment() {

    private val handler = Handler(Looper.getMainLooper())
    private var spinRunnable: Runnable? = null

    private lateinit var names: List<String>
    private lateinit var tvResult: TextView
    private lateinit var chipsContainer: LinearLayout

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        names = arguments?.getStringArrayList(ARG_NAMES)?.toList()
            ?.takeIf { it.isNotEmpty() } ?: DEFAULT_NAMES

        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_name_selector, null)

        tvResult = view.findViewById(R.id.tv_result_name)
        chipsContainer = view.findViewById(R.id.ll_chips_container)
        chipsContainer.removeAllViews()

        view.findViewById<View>(R.id.btn_close).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.btn_spin_again).setOnClickListener { spin() }

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
        setupDialogSize(R.fraction.dialog_width_small)
        spin()
    }

    override fun onDestroyView() {
        spinRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroyView()
    }

    private fun spin() {
        spinRunnable?.let { handler.removeCallbacks(it) }
        if (names.isEmpty()) return

        val totalTicks = 18 + Random.nextInt(6)
        val finalIndex = Random.nextInt(names.size)
        var tick = 0

        val runnable = object : Runnable {
            override fun run() {
                val index = if (tick >= totalTicks - 1) finalIndex else Random.nextInt(names.size)
                tvResult.text = names[index]
                tick++

                if (tick < totalTicks) {
                    val delay = (40L + tick * 12L).coerceAtMost(300L)
                    handler.postDelayed(this, delay)
                } else {
                    bounce(tvResult)
                    addToHistory(names[finalIndex])
                }
            }
        }
        spinRunnable = runnable
        handler.post(runnable)
    }

    private fun addToHistory(name: String) {
        for (i in 0 until chipsContainer.childCount) {
            val chip = chipsContainer.getChildAt(i) as TextView
            chip.setTextColor(Color.parseColor(COLOR_CHIP_TEXT))
            chip.background = pillDrawable(Color.parseColor(COLOR_CHIP_BG))
        }

        val chip = TextView(requireContext()).apply {
            text = name
            textSize = 14f
            setPadding(dp(20), dp(10), dp(20), dp(10))
            setTextColor(Color.WHITE)
            background = pillDrawable(Color.parseColor(COLOR_ACCENT))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(6)
                marginEnd = dp(6)
            }
        }
        chipsContainer.addView(chip)

        (chipsContainer.parent as? HorizontalScrollView)?.post {
            (chipsContainer.parent as HorizontalScrollView).fullScroll(View.FOCUS_RIGHT)
        }
    }

    private fun bounce(view: View) {
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
    }

    private fun pillDrawable(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(999).toFloat()
        setColor(color)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val ARG_NAMES = "arg_names"

        private const val COLOR_ACCENT = "#1A4479"
        private const val COLOR_CHIP_BG = "#EEF0F3"
        private const val COLOR_CHIP_TEXT = "#9AA0A6"

        val DEFAULT_NAMES = listOf("Diego", "Álvaro", "Marta", "Lucía", "Pablo")

        fun newInstance(names: List<String> = DEFAULT_NAMES): NameSelectorDialog {
            return NameSelectorDialog().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_NAMES, ArrayList(names))
                }
            }
        }
    }
}