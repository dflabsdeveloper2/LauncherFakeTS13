package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.util.setupDialogSize

/**
 * Diálogo "Temporizador" (cuenta atrás) con estilo visual unificado.
 */
class TimerDialog : DialogFragment() {

    private var countDownTimer: CountDownTimer? = null
    private var isRunning = false
    private var configuredSeconds = DEFAULT_SECONDS
    private var remainingSeconds = DEFAULT_SECONDS

    private lateinit var tvTime: TextView
    private lateinit var presetChips: List<TextView>
    private lateinit var btnMinus30: TextView
    private lateinit var btnPlus30: TextView
    private lateinit var btnStart: TextView
    private lateinit var btnReset: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_timer, null)

        tvTime = view.findViewById(R.id.tv_timer_time)
        presetChips = listOf(
            view.findViewById(R.id.chip_1min),
            view.findViewById(R.id.chip_3min),
            view.findViewById(R.id.chip_5min),
            view.findViewById(R.id.chip_10min),
            view.findViewById(R.id.chip_15min)
        )
        btnMinus30 = view.findViewById(R.id.btn_minus_30)
        btnPlus30 = view.findViewById(R.id.btn_plus_30)
        btnStart = view.findViewById(R.id.btn_start)
        btnReset = view.findViewById(R.id.btn_reset)

        presetChips.forEach { chip ->
            chip.setOnClickListener { selectPreset(chip) }
        }
        highlightPreset(presetChips[3]) // 10 min por defecto

        btnMinus30.setOnClickListener { adjustSeconds(-30) }
        btnPlus30.setOnClickListener { adjustSeconds(30) }
        btnStart.setOnClickListener { startCountdown() }
        btnReset.setOnClickListener { resetCountdown() }

        view.findViewById<View>(R.id.btn_close_timer).setOnClickListener { dismiss() }

        updateTimeText(configuredSeconds)

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
        setupDialogSize(R.fraction.dialog_width_medium)
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        super.onDestroyView()
    }

    private fun selectPreset(chip: TextView) {
        if (isRunning) return
        val minutes = chip.tag.toString().toInt()
        configuredSeconds = minutes * 60
        remainingSeconds = configuredSeconds
        highlightPreset(chip)
        updateTimeText(remainingSeconds)
    }

    private fun highlightPreset(selected: TextView?) {
        presetChips.forEach { chip ->
            if (chip == selected) {
                chip.setTextColor(Color.WHITE)
                chip.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_name_selector_button)
            } else {
                chip.setTextColor(Color.parseColor(COLOR_CHIP_TEXT))
                chip.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_timer_chip_unselected)
            }
        }
    }

    private fun adjustSeconds(delta: Int) {
        if (isRunning) return
        configuredSeconds = (configuredSeconds + delta).coerceAtLeast(0)
        remainingSeconds = configuredSeconds
        highlightPreset(null)
        updateTimeText(remainingSeconds)
    }

    private fun startCountdown() {
        if (isRunning || remainingSeconds <= 0) return
        isRunning = true
        setControlsEnabled(false)

        countDownTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000L).toInt() + 1
                updateTimeText(remainingSeconds)
            }

            override fun onFinish() {
                isRunning = false
                configuredSeconds = DEFAULT_SECONDS
                remainingSeconds = DEFAULT_SECONDS
                updateTimeText(remainingSeconds)
                highlightPreset(presetChips[3]) // 10 min
                setControlsEnabled(true)
            }
        }.start()
    }

    private fun resetCountdown() {
        countDownTimer?.cancel()
        isRunning = false
        remainingSeconds = configuredSeconds
        updateTimeText(remainingSeconds)
        setControlsEnabled(true)
    }

    private fun setControlsEnabled(enabled: Boolean) {
        presetChips.forEach { it.isEnabled = enabled }
        btnMinus30.isEnabled = enabled
        btnPlus30.isEnabled = enabled
        btnStart.isEnabled = enabled
        btnStart.alpha = if (enabled) 1f else 0.5f
    }

    private fun updateTimeText(seconds: Int) {
        val minutes = seconds / 60
        val secs = seconds % 60
        tvTime.text = String.format("%02d:%02d", minutes, secs)
    }

    companion object {
        private const val DEFAULT_SECONDS = 10 * 60
        private const val COLOR_CHIP_TEXT = "#5F6368"

        fun newInstance() = TimerDialog()
    }
}