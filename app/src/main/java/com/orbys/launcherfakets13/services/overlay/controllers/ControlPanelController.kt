package com.orbys.launcherfakets13.services.overlay.controllers

import android.app.Instrumentation
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.databinding.ViewVolBrightOverlayBinding
import com.orbys.launcherfakets13.ui.util.dp
import com.skg.services.manager.SkgPictureManager
import com.skg.services.manager.SkgSensorManager
import com.skg.services.manager.SkgSourceManager
import com.skg.services.manager.SkgVoiceManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Estado inicial del panel leído del SDK del fabricante. Los valores por defecto son los que se
 * muestran instantáneamente al abrir el panel (ver [ControlPanelController.show]) mientras la
 * lectura real llega en segundo plano — o si no llega nunca, ver [ControlPanelController.loadPanelSnapshotAsync].
 */
private data class PanelSnapshot(
    val volume: Int = 50,
    val isMuted: Boolean = false,
    val brightness: Int = 50,
    val isAutoBrightness: Boolean = false,
    val isEyeCareActive: Boolean = false,
    val isTouchSoundEnabled: Boolean = false
)

class ControlPanelController(context: Context) : BaseOverlayController(context) {

    private var _binding: ViewVolBrightOverlayBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("ControlPanelController binding is null. Is the view showing?")

    private val voiceManager = SkgVoiceManager.getInstance()
    private val sensorManager = SkgSensorManager.getInstance()
    private val pictureManager = SkgPictureManager.getInstance()
    private val sourceManager = SkgSourceManager.getInstance()

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isVisible() && _binding != null) {
                //syncUIWithSystem()
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun toggle(sidebarW: Int, sidebarY: Int, sidebarH: Int, fromRight: Boolean) {
        if (isVisible()) {
            removeView()
        } else {
            show(sidebarW, sidebarY, sidebarH, fromRight)
        }
    }

    private fun show(sidebarW: Int, sidebarY: Int, sidebarH: Int, fromRight: Boolean) {
        val b = ViewVolBrightOverlayBinding.inflate(LayoutInflater.from(context))
        _binding = b
        val newView = b.root

        val params = WindowManager.LayoutParams(
            260.dp,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            title = "OrbysControlPanel"
            if (fromRight) {
                b.root.background = ContextCompat.getDrawable(context, R.drawable.bg_side_panel_right)
                gravity = Gravity.END or Gravity.TOP
                x = sidebarW
            } else {
                b.root.background = ContextCompat.getDrawable(context, R.drawable.bg_sidebar_panel)
                gravity = Gravity.START or Gravity.TOP
                x = sidebarW
            }
            y = sidebarY
        }

        // El panel se muestra ya mismo con valores neutros (ver PanelSnapshot) — la lectura real
        // del SDK del fabricante llega en segundo plano y con timeout (ver loadPanelSnapshotAsync),
        // para que un SDK que no responda bien en este hardware/Android 13 nunca congele el
        // hilo principal ni retrase la apertura del panel.
        setupSliders(b, PanelSnapshot())
        setupExtraButtons(b, PanelSnapshot())
        setupPowerButtons(b)

        addViewSafely(newView, params)

        newView.post {
            if (_binding == null) return@post
            val panelH = newView.height
            layoutParams?.y = (sidebarY + sidebarH / 2 - panelH / 2).coerceAtLeast(0)
            updateViewSafely()
        }

        newView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) removeView()
            false
        }

        loadPanelSnapshotAsync { snapshot ->
            if (_binding !== b) return@loadPanelSnapshotAsync
            setupSliders(b, snapshot)
            setupExtraButtons(b, snapshot)
        }

        // Start periodic sync
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }

    /**
     * Lee el estado inicial del panel (volumen, brillo, sensores...) en un hilo de fondo. Si el
     * SDK del fabricante no responde en 800ms (p. ej. porque espera una conexión a un servicio/HAL
     * que no existe en esta imagen), se abandona la espera sin bloquear nada: el panel se queda
     * con los valores neutros ya mostrados por [show] en vez de congelarse indefinidamente.
     */
    private fun loadPanelSnapshotAsync(onResult: (PanelSnapshot) -> Unit) {
        val mainHandler = Handler(Looper.getMainLooper())
        val delivered = AtomicBoolean(false)

        Thread {
            val snapshot = PanelSnapshot(
                volume = runCatching { voiceManager.volume }.getOrDefault(50),
                isMuted = runCatching { voiceManager.muteState }.getOrDefault(false),
                brightness = runCatching { pictureManager.backLight }.getOrDefault(50),
                isAutoBrightness = runCatching { sensorManager.isLightSensorEnable }.getOrDefault(false),
                isEyeCareActive = runCatching { pictureManager.eyeCareModeStatus }.getOrDefault(false),
                isTouchSoundEnabled = runCatching { voiceManager.touchSoundState }.getOrDefault(false)
            )
            runCatching { pictureManager.setEyeCareModeOption(3) }
            if (delivered.compareAndSet(false, true)) {
                mainHandler.post { onResult(snapshot) }
            }
        }.start()

        mainHandler.postDelayed({ delivered.set(true) }, 800)
    }

    private fun setupSliders(b: ViewVolBrightOverlayBinding, snapshot: PanelSnapshot) {
        // Volume
        b.sbVolume.max = 100
        b.sbVolume.progress = snapshot.volume

        updateMuteUI(snapshot.isMuted, b)

        b.sbVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                if (fromUser) {
                    runCatching {
                        voiceManager.setVolume(value, false)
                        voiceManager.setMuteState(value == 0, false)
                        updateMuteUI(voiceManager.muteState, b)
                    }
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        b.btnMute.setOnClickListener {
            runCatching {
                val isMute = voiceManager.muteState
                voiceManager.setMuteState(!isMute, false)
                if (voiceManager.muteState) {
                    b.sbVolume.progress = 0
                } else {
                    if (b.sbVolume.progress == 0) {
                        voiceManager.setVolume(20, false)
                        b.sbVolume.progress = 20
                    }
                }
                updateMuteUI(voiceManager.muteState, b)
            }
        }

        // Brightness
        b.sbBrightness.max = 100
        b.sbBrightness.progress = snapshot.brightness

        updateAutoBrightnessUI(snapshot.isAutoBrightness, b)
        b.sbBrightness.isEnabled = !snapshot.isEyeCareActive

        b.sbBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                if (!fromUser) return
                runCatching {
                    if (!sensorManager.isLightSensorEnable) {
                        pictureManager.backLight = value
                    } else {
                        sensorManager.isLightSensorEnable = false
                        pictureManager.backLight = value
                        updateAutoBrightnessUI(false, b)
                    }
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        b.btnAutoBrightness.setOnClickListener {
            runCatching {
                if (pictureManager.eyeCareModeStatus) {
                    sensorManager.isLightSensorEnable = false
                    return@runCatching
                }

                val nextMode = !sensorManager.isLightSensorEnable
                sensorManager.isLightSensorEnable = nextMode
                if (nextMode) {
                    // When enabling auto, update slider immediately from current sensor value if possible
                    // SkgSensorManager.getInstance().sensorLightValue might be raw,
                    // usually manager updates pictureManager.backLight.
                    b.sbBrightness.progress = pictureManager.backLight
                }
                updateAutoBrightnessUI(nextMode, b)
            }
        }
    }

    // No se llama actualmente (ver updateRunnable) -- si se reactiva, debería pasar por el mismo
    // camino async con timeout que loadPanelSnapshotAsync en vez de leer los managers en el
    // hilo principal en un bucle cada segundo.
    private fun syncUIWithSystem() {
        val b = _binding ?: return
        runCatching {
            // Sync Volume if changed externally
            val currentVol = voiceManager.volume
            if (currentVol != b.sbVolume.progress) {
                b.sbVolume.progress = currentVol
                updateMuteUI(voiceManager.muteState, b)
            }

            // Sync Brightness if in auto mode or changed externally
            val currentBright = pictureManager.backLight
            if (currentBright != b.sbBrightness.progress) {
                b.sbBrightness.progress = currentBright
            }

            // Sync states
            updateAutoBrightnessUI(sensorManager.isLightSensorEnable, b)
            updateEyeCareUI(pictureManager.eyeCareModeStatus, b)
            updateTouchSoundUI(voiceManager.touchSoundState, b)
        }
    }

    private fun setupExtraButtons(b: ViewVolBrightOverlayBinding, snapshot: PanelSnapshot) {
        updateEyeCareUI(snapshot.isEyeCareActive, b)
        updateTouchSoundUI(snapshot.isTouchSoundEnabled, b)

        b.btnEyecare.isEnabled = false
        b.btnEyecare.alpha = 0.5f

        /*binding.btnEyecare.setOnClickListener {
            val nextState = !pictureManager.eyeCareModeStatus
            pictureManager.eyeCareModeStatus = nextState
            pictureManager.setEyeCareModeOption(3)
            updateEyeCareUI(nextState)
        }*/

        b.btnTouchsound.setOnClickListener {
            runCatching {
                val nextState = !voiceManager.touchSoundState
                voiceManager.touchSoundState = nextState
                updateTouchSoundUI(nextState, b)
            }
        }
    }

    private fun setupPowerButtons(b: ViewVolBrightOverlayBinding) {
        b.btnOpsOff.setOnClickListener { shutdownOps() }
        b.btnReboot.setOnClickListener { rebootDevice() }
        b.btnPowerOff.setOnClickListener { powerOff() }
    }

    private fun shutdownOps() {
        if (sourceManager.opsSignalStatus) {
            Thread {
                try {
                    val inst = Instrumentation()
                    inst.sendKeyDownUpSync(606) // KEYCODE_SKG_SHUTDOWN_OPS_ONLY
                } catch (e: Exception) {
                    Log.d("OPS", "Apagar OPS Exception: $e")
                }
            }.start()
        }
    }

    private fun powerOff() {
        Thread {
            try {
                val inst = Instrumentation()
                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_POWER)
            } catch (e: Exception) {
                Log.d("POWER", "Exception $e")
            }
        }.start()
    }

    private fun rebootDevice() {
        try {
            context.getSystemService(PowerManager::class.java).reboot(null)
        } catch (e: Exception) {
            Log.d("REBOOT", "Exception $e")
        }
    }

    private fun updateMuteUI(isMuted: Boolean, b: ViewVolBrightOverlayBinding? = _binding) {
        val binding = b ?: return
        val color = if (isMuted) {
            ContextCompat.getColor(context, R.color.item_selected)
        } else {
            ContextCompat.getColor(context, R.color.dock_text_inactive)
        }
        binding.btnMute.setColorFilter(color)
        binding.btnMute.setImageResource(if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume)
    }

    private fun updateAutoBrightnessUI(isAuto: Boolean, b: ViewVolBrightOverlayBinding? = _binding) {
        val binding = b ?: return
        val color = if (isAuto) {
            ContextCompat.getColor(context, R.color.item_selected)
        } else {
            ContextCompat.getColor(context, R.color.dock_text_inactive)
        }
        binding.btnAutoBrightness.setColorFilter(color)
        binding.btnAutoBrightness.setImageResource(if (isAuto) R.drawable.ic_brightness_auto else R.drawable.ic_brightness)
    }

    private fun updateEyeCareUI(isActive: Boolean, b: ViewVolBrightOverlayBinding? = _binding) {
        val binding = b ?: return
        val color = if (isActive) {
            ContextCompat.getColor(context, R.color.item_selected)
        } else {
            ContextCompat.getColor(context, R.color.dock_text_inactive)
        }
        binding.ivEyecare.setColorFilter(color)
        binding.tvEyecare.setTextColor(color)
    }

    private fun updateTouchSoundUI(isEnabled: Boolean, b: ViewVolBrightOverlayBinding? = _binding) {
        val binding = b ?: return
        val color = if (isEnabled) {
            ContextCompat.getColor(context, R.color.item_selected)
        } else {
            ContextCompat.getColor(context, R.color.dock_text_inactive)
        }
        binding.ivTouchsound.setColorFilter(color)
        binding.tvTouchsound.setTextColor(color)
    }

    override fun removeView() {
        handler.removeCallbacks(updateRunnable)
        super.removeView()
        _binding = null
    }
}
