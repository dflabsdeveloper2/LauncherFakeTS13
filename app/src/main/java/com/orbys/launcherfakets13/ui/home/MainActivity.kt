package com.orbys.launcherfakets13.ui.home

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.databinding.ActivityMainBinding
import com.orbys.launcherfakets13.domain.model.Environment
import com.orbys.launcherfakets13.services.ConnectivityReceiver
import com.orbys.launcherfakets13.services.Broadcaster
import com.orbys.launcherfakets13.services.overlay.DockOverlayService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.ui.dialog.AdminDisabledDialog
import com.orbys.launcherfakets13.ui.dialog.EnvironmentSelectorDialog
import com.orbys.launcherfakets13.ui.dialog.WallpaperOptionsDialog
import com.orbys.launcherfakets13.ui.util.EnvironmentMapper
import com.orbys.launcherfakets13.util.SystemActionHelper
import com.orbys.launcherfakets13.util.WidgetPickerHelper
import com.skg.services.manager.SkgSettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var connectivityReceiver: ConnectivityReceiver

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.refreshAll()
        }
    }

    private val wallpaperManager: WallpaperManager by lazy { WallpaperManager.getInstance(this) }
    private val appWidgetManager: AppWidgetManager by lazy { AppWidgetManager.getInstance(this) }

    private var currentEnviroment = Environment.OFFICE

    // ── Activity result launchers ─────────────────────────────────────────────


    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) DockOverlayService.start(this)
    }

    private val notificationListenerPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    // Handles ACTION_APPWIDGET_BIND result (user confirmed binding)
    private val widgetPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        widgetPickerHelper?.onBindResult(result.resultCode == RESULT_OK)
    }

    private val widgetConfigureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        widgetPickerHelper?.onConfigureResult(result.resultCode == RESULT_OK)
    }

    private var widgetPickerHelper: WidgetPickerHelper? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Home es la página visible por defecto; Desktop queda aparcado fuera de pantalla a la derecha.
        binding.desktopContainerView.translationX = resources.displayMetrics.widthPixels.toFloat()

        setupWallpaper()
        setupSearchBar()

        connectivityReceiver = ConnectivityReceiver { status ->
            viewModel.updateWifiStatus(status.isWifiConnected)
            viewModel.updateEthernetStatus(status.isEthernetConnected)
            viewModel.updateHotspotStatus(status.isHotspotEnabled)
            viewModel.updateUsbStatus(status.isUsbConnected)
            viewModel.updateBluetoothStatus(status.isBluetoothEnabled)
        }
        val initialStatus = connectivityReceiver.getConnectivityStatus(this)
        viewModel.updateWifiStatus(initialStatus.isWifiConnected)
        viewModel.updateEthernetStatus(initialStatus.isEthernetConnected)
        viewModel.updateHotspotStatus(initialStatus.isHotspotEnabled)
        viewModel.updateUsbStatus(initialStatus.isUsbConnected)
        viewModel.updateBluetoothStatus(initialStatus.isBluetoothEnabled)

        binding.profileSelectorCard.setOnClickListener { showProfileSelectorDialog() }
        
        binding.ivWifi.setOnClickListener {
            DockOverlayService.minimize()
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }

        binding.ivUsb.setOnClickListener {
            DockOverlayService.minimize()
            openFileManager()
        }

        binding.ivHotspot.setOnClickListener {
            DockOverlayService.minimize()
            startActivity(Intent().apply {
                component = android.content.ComponentName("com.android.settings", "com.android.settings.Settings\$TetherSettingsActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }

        binding.ivBluetooth.setOnClickListener {
            DockOverlayService.minimize()
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }

        binding.ivUserProfile.setOnClickListener {
            AdminDisabledDialog.newInstance().show(supportFragmentManager, "admin_disabled")
        }

        binding.btnExitTestMode.setOnClickListener {
            finish()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateConnectivityIcons(state)
                    currentEnviroment = state.currentEnvironment
                    updateProfileUI(state.currentEnvironment)
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, desktopBackCallback)

        try {
            Log.d("API_INTERNA", "Checking SKG internal api,   SN: ${skgSettings()?.serialNumber}, }")
        }catch ( e: Exception) {
            Log.e("API_INTERNA", "Exception Api: $e")
        }
    }

    private fun skgSettings(): SkgSettingsManager? = try {
        SkgSettingsManager.getInstance()
    } catch (e: Exception) {
        Log.e("API_INTERNA", "SkgSettingsManager no disponible Exception: $e")
        null
    }

    // Único punto de la app por el que pasa CADA MotionEvent exactamente una vez, sin importar
    // quién lo consuma después (Activity.dispatchTouchEvent es el punto de entrada de todo el
    // árbol de Views) — por eso es la única fuente fiable de "hay un dedo pulsado ahora mismo".
    // DesktopFragment lo consulta para no reconstruir su grid mientras un gesto sigue en curso
    // (ver isPointerDown()/flushPendingRebuild()); un contador local a nivel de card/contenedor
    // se probó antes y resultó frágil: si un DOWN no lo "reclama" ningún View (contenido sin
    // superficie táctil en esa zona), Android lo reenvía como fallback a otro receptor, así que
    // se podía contar el inicio dos veces pero el final solo una, dejando el contador atascado.
    private var isPointerDown = false
    fun isPointerDown() = isPointerDown

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> isPointerDown = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isPointerDown = false
        }
        // wallpaperGestureDetector vive a nivel de Activity/Window, fuera de la cadena de Views:
        // requestDisallowInterceptTouchEvent no le afecta, así que si se le sigue alimentando el
        // gesto mientras una card está en modo edición, un arrastre rápido hacia un lado (mover
        // un widget) se puede confundir con el fling de cambio de página Home<->Desktop y
        // navegar a media edición. Se salta por completo mientras la página visible reporta
        // edición en curso.
        val isEditingCurrentPage = if (isDesktopVisible) {
            getDesktopFragment()?.isEditing() == true
        } else {
            getWidgetFragment()?.isEditing() == true
        }
        if (!isEditingCurrentPage) wallpaperGestureDetector?.onTouchEvent(ev)
        val result = super.dispatchTouchEvent(ev)
        // Al soltar el dedo, avisar a DesktopFragment por si tenía una reconstrucción de grid en
        // cola (ver DesktopFragment.pendingRebuildItems) — post() para que corra después de que
        // este dispatch, y todo lo que dependa de él en el árbol de Views, termine del todo.
        if (!isPointerDown && (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL)) {
            window.decorView.post { getDesktopFragment()?.flushPendingRebuild() }
        }
        return result
    }

    override fun onStart() {
        super.onStart()
        Broadcaster.sendOpen(this)
        startDockService()
        requestNotificationListenerAccessIfNeeded()
        registerReceiver(connectivityReceiver, ConnectivityReceiver.getGeneralFilter(), RECEIVER_NOT_EXPORTED)
        registerReceiver(connectivityReceiver, ConnectivityReceiver.getMediaFilter(), RECEIVER_NOT_EXPORTED)

        val pkgFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, pkgFilter)
    }

    override fun onStop() {
        super.onStop()
        Broadcaster.sendClose(this)
        DockOverlayService.stop(this)
        unregisterReceiver(connectivityReceiver)
        unregisterReceiver(packageReceiver)
    }

    override fun onResume() {
        super.onResume()
        hideNavBar()
        SystemActionHelper.setStatusBarLocked(this, true)

        if (isDesktopVisible) getDesktopFragment()?.onPageShown()

        // Detectar si el usuario cambió el wallpaper en el picker del sistema
        val currentId = wallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
        viewModel.checkWallpaperConsistency(currentId)
    }

    override fun onPause() {
        super.onPause()
        // Unlock on leaving the launcher: locking is only meant to apply to the home screen
        // itself, not device-wide. Without this, DISABLE_EXPAND stays set for every other
        // foreground app too, making the notification shade permanently unreachable (CDD 3.8.3).
        SystemActionHelper.setStatusBarLocked(this, false)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavBar()
    }

    private fun getWidgetFragment(): WidgetFragment? {
        return supportFragmentManager.findFragmentById(R.id.widget_container_view) as? WidgetFragment
    }

    // ── Widget picker ──────────────────────────────────────────────────────────

    fun launchWidgetPicker() {
        val host = getWidgetFragment()?.obtainAppWidgetHost() ?: return
        WidgetPickerHelper(
            context = this,
            appWidgetManager = appWidgetManager,
            host = host,
            bindLauncher = widgetPickerLauncher,
            configureLauncher = widgetConfigureLauncher,
            onBound = { id, info -> finalizeWidgetAdd(id, info) },
            onFailed = { Toast.makeText(this, R.string.widget_add_failed, Toast.LENGTH_SHORT).show() }
        ).also {
            widgetPickerHelper = it
            it.launchPicker()
        }
    }

    private fun finalizeWidgetAdd(id: Int, info: AppWidgetProviderInfo) {
        val widthDp  = info.minWidth.coerceAtLeast(48)
        val heightDp = info.minHeight.coerceAtLeast(48)
        val existing = viewModel.uiState.value.widgets.size
        val screenW  = resources.displayMetrics.widthPixels
        val screenH  = resources.displayMetrics.heightPixels
        val offsetPx = dpToPx(24) * existing
        val xDp = pxToDp(((screenW / 2 - dpToPx(widthDp) / 2) + offsetPx).coerceAtLeast(0))
        val yDp = pxToDp(((screenH / 4) + offsetPx).coerceAtLeast(0))
        viewModel.addWidget(id, xDp, yDp, widthDp, heightDp)
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun hideNavBar() {
        window.insetsController?.let {
            it.hide(WindowInsets.Type.navigationBars() or WindowInsets.Type.statusBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun startDockService() {
        if (Settings.canDrawOverlays(this)) {
            DockOverlayService.start(this)
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.overlay_permission_title)
                .setMessage(R.string.overlay_permission_msg)
                .setPositiveButton(R.string.go_to_settings) { _, _ ->
                    overlayPermissionLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = "package:$packageName".toUri()
                        }
                    )
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    /** CDD 3.8.1/H-SR-3: sin esto, LauncherNotificationListenerService nunca se conecta y los
     *  badges de notificación en los iconos quedan siempre a cero. No es un permiso normal:
     *  el usuario debe concederlo explícitamente desde Ajustes (no se puede pre-conceder vía
     *  privapp-permissions.xml como el resto de permisos privilegiados de esta app). */
    private fun requestNotificationListenerAccessIfNeeded() {
        val granted = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        if (granted) return

        AlertDialog.Builder(this)
            .setTitle(R.string.notification_listener_permission_title)
            .setMessage(R.string.notification_listener_permission_msg)
            .setPositiveButton(R.string.go_to_settings) { _, _ ->
                notificationListenerPermissionLauncher.launch(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openFileManager() {
        val candidates = listOf(
            "com.android.documentsui",
            "com.google.android.documentsui",
            "com.android.fileexplorer",
            "com.orbys.filemanager"
        )
        val launched = candidates.any { pkg ->
            packageManager.getLaunchIntentForPackage(pkg)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?.let { startActivity(it); true } ?: false
        }
        if (!launched) {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        "content://com.android.externalstorage.documents/root/primary".toUri(),
                        "vnd.android.document/root"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        }
    }

    // ── Wallpaper ─────────────────────────────────────────────────────────────

    private val wallpaperFile get() = File(filesDir, "wallpaper.jpg")
    private var wallpaperGestureDetector: GestureDetector? = null

    private fun setupWallpaper() {
        loadWallpaper()
        wallpaperGestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                // En la página Desktop el long-press abre el selector de App/Widget propio de esa página.
                if (!isDesktopVisible && isEmptyBackground(e.rawX, e.rawY)) showWallpaperDialog()
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val dx = e2.rawX - e1.rawX
                val dy = e2.rawY - e1.rawY
                if (abs(dx) <= abs(dy) || abs(dx) < dpToPx(80) || abs(velocityX) < 400) return false
                if (dx < 0 && !isDesktopVisible) {
                    showDesktop()
                    return true
                }
                if (dx > 0 && isDesktopVisible) {
                    hideDesktop()
                    return true
                }
                return false
            }
        })
    }

    // ── Desktop page (grid-snap, apps + widgets) ────────────────────────────────

    private var isDesktopVisible = false

    private val desktopBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = hideDesktop()
    }

    private fun getDesktopFragment(): DesktopFragment? {
        return supportFragmentManager.findFragmentById(R.id.desktop_container_view) as? DesktopFragment
    }

    /**
     * Home (izquierda) y Desktop (derecha) son dos paneles del mismo ancho de pantalla
     * que se desplazan a la vez, como un pager de dos páginas: cuando uno entra por un
     * lado el otro sale por el contrario.
     */
    private fun showDesktop() {
        if (isDesktopVisible) return
        isDesktopVisible = true
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        binding.homePageContainer.animate().translationX(-screenW).setDuration(220).start()
        binding.desktopContainerView.animate().translationX(0f).setDuration(220).start()
        getDesktopFragment()?.onPageShown()
        desktopBackCallback.isEnabled = true
    }

    private fun hideDesktop() {
        if (!isDesktopVisible) return
        isDesktopVisible = false
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        binding.homePageContainer.animate().translationX(0f).setDuration(220).start()
        binding.desktopContainerView.animate().translationX(screenW).setDuration(220).start()
        getDesktopFragment()?.onPageHidden()
        desktopBackCallback.isEnabled = false
    }

    private fun isEmptyBackground(rawX: Float, rawY: Float): Boolean {
        // Since we moved content to fragments, we should ideally check fragment views.
        // For now, simple check against widget/category containers if they were still here,
        // but they are now in fragments. We can just check the containers in the activity.
        val containers = listOf(R.id.category_container_view, R.id.widget_container_view)
        for (id in containers) {
            findViewById<View>(id) ?: continue
        }
        return true // Simplified for now
    }

    private fun showWallpaperDialog() {
        WallpaperOptionsDialog.newInstance().apply {
            onChangeWallpaper = {
                // Guardamos el ID actual antes de irnos para comparar al volver
                viewModel.prepareForWallpaperChange(wallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM))
                val intent = Intent("android.settings.WALLPAPER_SETTINGS")
                startActivity(intent)
            }
            onRemoveWallpaper = {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.wallpaper_remove)
                    .setMessage(R.string.wallpaper_remove_msg)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        viewModel.restoreDefaultWallpaper(currentEnviroment)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .create()
                    .also { dialog ->
                        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
                    }
                    .show()
            }
           /* onAddWidget = {
                launchWidgetPicker()
            }*/
        }.show(supportFragmentManager, "wallpaper_options")
    }

    private fun loadWallpaper() {
        // Si el tipo es CUSTOM, ocultamos esta ImageView para dejar ver el fondo del sistema
        if (viewModel.uiState.value.wallpaperType == com.orbys.launcherfakets13.domain.model.WallpaperType.CUSTOM) {
            binding.ivWallpaper.visibility = View.GONE
            return
        }

        val file = wallpaperFile
        if (file.exists()) {
            val bmp = decodeSampledBitmap(file.absolutePath)
            if (bmp != null) {
                binding.ivWallpaper.setImageBitmap(bmp)
                binding.ivWallpaper.visibility = View.VISIBLE
                return
            }
        }
        binding.ivWallpaper.visibility = View.GONE
        binding.ivWallpaper.setImageDrawable(null)
    }

    private fun decodeSampledBitmap(path: String): Bitmap? {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        var sample = 1
        var w = opts.outWidth
        var h = opts.outHeight
        while (w > screenW * 2 || h > screenH * 2) {
            sample *= 2
            w /= 2
            h /= 2
        }
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun applyWallpaper(uri: Uri) {
        runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(wallpaperFile).use { output -> input.copyTo(output) }
            }
            loadWallpaper()
        }.onFailure {
            Toast.makeText(this, R.string.wallpaper_change_failed, Toast.LENGTH_SHORT).show()
        }
    }

    // ── Search bar ───────────────────────────────────────────────────────────

    /** Dispositivo sin GMS: la barra de búsqueda simplemente abre Chrome al tocarla. */
    private fun setupSearchBar() {
        binding.searchBarContainer.setOnClickListener {
            AdminDisabledDialog.newInstance().show(supportFragmentManager, "admin_disabled")
        }
    }

    private fun updateConnectivityIcons(state: MainState) {
        binding.ivWifi.visibility = if (state.isWifiConnected) View.VISIBLE else View.GONE
        binding.ivEthernet.visibility = if (state.isEthernetConnected) View.VISIBLE else View.GONE
        binding.ivHotspot.visibility = if (state.isHotspotEnabled) View.VISIBLE else View.GONE
        binding.ivUsb.visibility = if (state.isUsbConnected) View.VISIBLE else View.GONE
        binding.ivBluetooth.visibility = if (state.isBluetoothEnabled) View.VISIBLE else View.GONE
    }

    private fun updateProfileUI(environment: Environment) {
        binding.tvProfileTitle.text = getString(EnvironmentMapper.getTitleRes(environment))
        binding.ivProfileIcon.setImageResource(EnvironmentMapper.getIconRes(environment))
        binding.tvProfileSubtitle.text = getString(EnvironmentMapper.getSubtitleRes(environment))

        // Si el modo es DEFAULT, aplicamos el fondo del entorno. 
        // Si es CUSTOM, mostramos el fondo del sistema habilitando FLAG_SHOW_WALLPAPER y quitando el fondo del root.
        if (viewModel.uiState.value.wallpaperType == com.orbys.launcherfakets13.domain.model.WallpaperType.DEFAULT) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            binding.root.setBackgroundResource(EnvironmentMapper.getBackgroundRes(environment))
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            binding.root.setBackgroundResource(android.R.color.transparent)
        }
    }

    private fun showProfileSelectorDialog() {
        EnvironmentSelectorDialog.newInstance(viewModel.uiState.value.currentEnvironment).apply {
            onEnvironmentSelected = { env -> viewModel.updateEnvironment(env) }
        }.show(supportFragmentManager, "env_selector")
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
    private fun pxToDp(px: Int) = (px / resources.displayMetrics.density).toInt()
}
