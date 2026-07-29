package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.services.overlay.DockOverlayService
import java.util.Calendar
import kotlin.random.Random

/**
 * Diálogo de pantalla completa "Modo Concentración": reloj del sistema y un
 * medidor de decibelios simulado (barras animadas + valor en dB), con un
 * botón para salir.
 */
class FocusModeDialog : DialogFragment() {

    private val handler = Handler(Looper.getMainLooper())
    private var clockRunnable: Runnable? = null
    private var meterRunnable: Runnable? = null

    private lateinit var tvClock: TextView
    private lateinit var tvDbValue: TextView
    private lateinit var tvDbLabel: TextView
    private lateinit var barsContainer: LinearLayout
    private lateinit var bars: List<View>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_focus_mode, null)

        tvClock = view.findViewById(R.id.tv_focus_clock)
        tvDbValue = view.findViewById(R.id.tv_focus_db_value)
        tvDbLabel = view.findViewById(R.id.tv_focus_db_label)
        barsContainer = view.findViewById(R.id.ll_focus_bars)

        buildBars()

        view.findViewById<View>(R.id.btn_exit_focus_mode).setOnClickListener { dismiss() }

        return Dialog(requireContext(), R.style.CustomDialogTheme).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(view)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setElevation(0f)
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            val bounds = requireActivity().windowManager.currentWindowMetrics.bounds
            setLayout(bounds.width(), bounds.height())
            WindowCompat.setDecorFitsSystemWindows(this, false)
        }
        DockOverlayService.setDockVisibility(false)
        startClock()
        startMeter()
    }

    override fun onDestroyView() {
        DockOverlayService.setDockVisibility(true)
        clockRunnable?.let { handler.removeCallbacks(it) }
        meterRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroyView()
    }

    private fun buildBars() {
        barsContainer.removeAllViews()
        bars = (0 until BAR_COUNT).map {
            View(requireContext()).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(6).toFloat()
                    setColor(Color.parseColor(COLOR_BAR))
                }
                layoutParams = LinearLayout.LayoutParams(dp(10), dp(MIN_BAR_DP)).apply {
                    marginStart = dp(5)
                    marginEnd = dp(5)
                }
            }
        }
        bars.forEach { barsContainer.addView(it) }
    }

    private fun startClock() {
        val runnable = object : Runnable {
            override fun run() {
                val calendar = Calendar.getInstance()
                tvClock.text = String.format(
                    "%02d:%02d",
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE)
                )
                handler.postDelayed(this, 1000L)
            }
        }
        clockRunnable = runnable
        handler.post(runnable)
    }

    private fun startMeter() {
        val runnable = object : Runnable {
            override fun run() {
                val db = Random.nextInt(28, 62)
                tvDbValue.text = db.toString()
                tvDbLabel.text = labelFor(db)

                bars.forEach { bar ->
                    val jitter = Random.nextInt(-20, 21)
                    val level = (db + jitter).coerceIn(6, 100)
                    val heightDp = MIN_BAR_DP + (level * (MAX_BAR_DP - MIN_BAR_DP) / 100)
                    (bar.layoutParams as LinearLayout.LayoutParams).height = dp(heightDp)
                    bar.requestLayout()
                }

                handler.postDelayed(this, 350L)
            }
        }
        meterRunnable = runnable
        handler.post(runnable)
    }

    private fun labelFor(db: Int): String = when {
        db < 35 -> "AMBIENTE MUY TRANQUILO"
        db < 50 -> "AMBIENTE TRANQUILO"
        db < 65 -> "AMBIENTE MODERADO"
        else -> "AMBIENTE RUIDOSO"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val BAR_COUNT = 13
        private const val MIN_BAR_DP = 10
        private const val MAX_BAR_DP = 90
        private const val COLOR_BAR = "#7DEDD0"

        fun newInstance() = FocusModeDialog()
    }
}
