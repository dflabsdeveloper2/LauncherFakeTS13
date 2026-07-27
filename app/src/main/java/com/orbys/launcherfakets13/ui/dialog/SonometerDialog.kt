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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.util.setupDialogSize
import kotlin.random.Random

/**
 * Diálogo "Sonómetro" (medidor de ruido) animado.
 */
class SonometerDialog : DialogFragment() {

    private val handler = Handler(Looper.getMainLooper())
    private var meterRunnable: Runnable? = null

    private lateinit var tvDbValue: TextView
    private lateinit var tvDbLabel: TextView
    private lateinit var barsContainer: LinearLayout
    private lateinit var bars: List<View>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_sonometer, null)

        tvDbValue = view.findViewById(R.id.tv_noise_db_value)
        tvDbLabel = view.findViewById(R.id.tv_noise_db_label)
        barsContainer = view.findViewById(R.id.ll_noise_bars)

        buildBars()

        view.findViewById<View>(R.id.btn_close_sonometer).setOnClickListener { dismiss() }

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
        setupDialogSize(R.fraction.dialog_width_small)
        startMeter()
    }

    override fun onDestroyView() {
        meterRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroyView()
    }

    private fun buildBars() {
        barsContainer.removeAllViews()
        bars = (0 until BAR_COUNT).map {
            View(requireContext()).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(14).toFloat()
                    setColor(Color.parseColor(COLOR_BAR))
                }
                layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(MIN_BAR_DP)).apply {
                    marginStart = dpToPx(6)
                    marginEnd = dpToPx(6)
                }
            }
        }
        bars.forEach { barsContainer.addView(it) }
    }

    private fun startMeter() {
        val runnable = object : Runnable {
            override fun run() {
                val db = Random.nextInt(20, 80)
                tvDbValue.text = "$db dB"
                tvDbLabel.text = labelFor(db)

                bars.forEachIndexed { index, bar ->
                    val trend = index * 6
                    val jitter = Random.nextInt(-15, 16)
                    val level = (db + trend + jitter).coerceIn(8, 100)
                    val heightDp = MIN_BAR_DP + (level * (MAX_BAR_DP - MIN_BAR_DP) / 100)
                    (bar.layoutParams as LinearLayout.LayoutParams).height = dpToPx(heightDp)
                    bar.requestLayout()
                }

                handler.postDelayed(this, 500L)
            }
        }
        meterRunnable = runnable
        handler.post(runnable)
    }

    private fun labelFor(db: Int): String = when {
        db < 35 -> "BAJO"
        db < 55 -> "MODERADO"
        db < 70 -> "ALTO"
        else -> "MUY ALTO"
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    companion object {
        private const val BAR_COUNT = 5
        private const val MIN_BAR_DP = 20
        private const val MAX_BAR_DP = 130
        private const val COLOR_BAR = "#F1585E"

        fun newInstance() = SonometerDialog()
    }
}