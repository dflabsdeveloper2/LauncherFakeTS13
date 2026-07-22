package com.orbys.launcherts13.services.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import com.orbys.launcherts13.R
import com.orbys.launcherts13.data.repository.AppsRepositoryImpl
import com.orbys.launcherts13.services.overlay.controllers.AppDrawerController
import com.orbys.launcherts13.services.overlay.controllers.ControlPanelController
import com.orbys.launcherts13.services.overlay.controllers.DockController
import com.orbys.launcherts13.services.overlay.controllers.RecentsController
import com.orbys.launcherts13.services.overlay.controllers.SidebarController
import java.lang.ref.WeakReference

class DockOverlayService : Service() {

    private lateinit var themedCtx: Context

    private lateinit var dockController: DockController
    private lateinit var sidebarLeftController: SidebarController
    private lateinit var sidebarRightController: SidebarController
    private lateinit var drawerController: AppDrawerController
    private lateinit var controlPanelController: ControlPanelController
    private lateinit var recentsController: RecentsController

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_LOCALE_CHANGED) {
                refreshUI()
            }
        }
    }

    private var receiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIF_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.e("DockOverlay", "SYSTEM_ALERT_WINDOW not granted — stopping service")
            stopSelf()
            return
        }

        instance = WeakReference(this)
        themedCtx = ContextThemeWrapper(this, R.style.Theme_LauncherTs13)

        // Precalienta la caché de apps instaladas en segundo plano, antes de que el
        // usuario llegue a tocar el icono de apps del dock.
        AppsRepositoryImpl.prefetch(applicationContext)

        initControllers()

        dockController.show()
        sidebarLeftController.show()
        sidebarRightController.show()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        registerReceiver(timeReceiver, filter, RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
    }

    private fun initControllers() {
        dockController = DockController(
            themedCtx,
            onAppsTabClick = { drawerController.toggle() },
            onFilesTabClick = { openFileManager() },
            onPizarraTabClick = { openPizarra() },
            onBrowserTabClick = { openBrowser() },
            onSettingsTabClick = { openSettings() },
            onToggleDock = { syncSidebarsDockIcon() }
        )

        sidebarLeftController = SidebarController(
            themedCtx,
            SidebarController.Side.LEFT,
            onTuneClick = { w, y, h, fromRight ->
                controlPanelController.toggle(
                    w,
                    y,
                    h,
                    fromRight
                )
            },
            onToggleDock = { dockController.toggle() },
            onExpandSync = { expandAll() },
            onCollapseSync = { collapseSidebars() },
            isDockExpanded = { dockController.isExpanded }
        )

        sidebarRightController = SidebarController(
            themedCtx,
            SidebarController.Side.RIGHT,
            onTuneClick = { w, y, h, fromRight ->
                controlPanelController.toggle(
                    w,
                    y,
                    h,
                    fromRight
                )
            },
            onToggleDock = { dockController.toggle() },
            onExpandSync = { expandAll() },
            onCollapseSync = { collapseSidebars() },
            isDockExpanded = { dockController.isExpanded }
        )

        drawerController = AppDrawerController(
            themedCtx,
            getDockView = { dockController.rootView },
            onAppLaunched = { dockController.collapse() },
            onVisibilityChanged = { visible ->
                if (visible) dockController.selectTab(R.id.tab_apps)
                else dockController.clearTabSelection(R.id.tab_apps)
            }
        )

        controlPanelController = ControlPanelController(themedCtx)

        recentsController = RecentsController(
            themedCtx,
            getDockView = { dockController.rootView },
            onVisibilityChanged = { visible ->
                if (visible) dockController.selectTab(R.id.tab_recents)
                else dockController.clearTabSelection(R.id.tab_recents)
            }
        )
    }

    private fun syncSidebarsDockIcon() {
        sidebarLeftController.syncDockIcon()
        sidebarRightController.syncDockIcon()
        
        if (dockController.isExpanded) {
            expandAll()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("DockOverlay", "onConfigurationChanged fired")
        refreshUI()
    }

    private var refreshing = false

    private fun refreshUI() {
        if (refreshing) {
            Log.w("DockOverlay", "refreshUI() re-entrant call ignored")
            return
        }
        refreshing = true
        Log.d("DockOverlay", "Refreshing UI...")

        try {
            themedCtx = ContextThemeWrapper(this, R.style.Theme_LauncherTs13)

            // Save states
            val wasDockExpanded = dockController.isExpanded

            // Remove and re-init
            dockController.removeView()
            sidebarLeftController.removeView()
            sidebarRightController.removeView()
            drawerController.removeView()
            controlPanelController.removeView()
            recentsController.removeView()

            initControllers()

            // Re-show
            dockController.show()
            if (!wasDockExpanded) dockController.collapse()

            sidebarLeftController.show()
            sidebarRightController.show()

            Log.d("DockOverlay", "Refreshing UI... done")
        } catch (e: Exception) {
            // Don't leave dockController/sidebar controllers half torn-down: whatever partial
            // state initControllers() reached, make sure the dock view still exists.
            Log.e("DockOverlay", "refreshUI() failed, re-showing dock as fallback", e)
            runCatching { dockController.show() }
        } finally {
            refreshing = false
        }
    }

    override fun onDestroy() {
        instance = null
        if (receiverRegistered) {
            unregisterReceiver(timeReceiver)
            receiverRegistered = false
        }
        dockController.removeView()
        sidebarLeftController.removeView()
        sidebarRightController.removeView()
        drawerController.removeView()
        controlPanelController.removeView()
        recentsController.removeView()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    // ── Helper app launchers ──────────────────────────────────────────────────

    private fun openFileManager() {
        val candidates = listOf(
            "com.android.documentsui",
            "com.google.android.documentsui",
            "com.android.fileexplorer"
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
                        Uri.parse("content://com.android.externalstorage.documents/root/primary"),
                        "vnd.android.document/root"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        }
    }

    private fun openPizarra() {
        val intent = packageManager.getLaunchIntentForPackage("com.skg.writer")
            ?: packageManager.getLaunchIntentForPackage("com.seewo.easinote")
        if (intent != null) {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            Log.d("DockOverlayService", "Pizarra app not found")
        }
    }

    private fun openBrowser() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun openSettings() {
        startActivity(Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_dock),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_dock_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_dock_active))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        private const val NOTIF_ID = 1001
        private const val CHANNEL_ID = "dock_overlay"
        private var instance: WeakReference<DockOverlayService>? = null

        fun minimize() {
            instance?.get()?.dockController?.collapse()
        }

        fun toggleDock() {
            instance?.get()?.dockController?.toggle()
        }

        fun showDock() {
            instance?.get()?.dockController?.expand()
        }

        fun expandAll() {
            val service = instance?.get() ?: return
            service.dockController.expand()
            service.sidebarLeftController.expand()
            service.sidebarRightController.expand()
        }

        fun collapseSidebars() {
            val service = instance?.get() ?: return
            service.sidebarLeftController.collapse()
            service.sidebarRightController.collapse()
        }

        fun isDockExpanded() = instance?.get()?.dockController?.isExpanded ?: true
        fun toggleVolBright(
            sidebarW: Int,
            sidebarY: Int,
            sidebarH: Int = 0,
            fromRight: Boolean = false
        ) {
            instance?.get()?.controlPanelController?.toggle(sidebarW, sidebarY, sidebarH, fromRight)
        }

        fun hideVolBright() {
            instance?.get()?.controlPanelController?.removeView()
        }

        fun toggleRecents() {
            instance?.get()?.recentsController?.toggle()
        }

        fun removeFallbackSidebar() {
            instance?.get()?.sidebarLeftController?.removeView()
        }

        fun restoreFallbackSidebar() {
            instance?.get()?.sidebarLeftController?.show()
        }

        fun removeFallbackSidebarRight() {
            instance?.get()?.sidebarRightController?.removeView()
        }

        fun restoreFallbackSidebarRight() {
            instance?.get()?.sidebarRightController?.show()
        }

        fun clearDockSelection() {
            instance?.get()?.dockController?.clearSelection()
        }

        fun start(context: Context) =
            context.startForegroundService(Intent(context, DockOverlayService::class.java))

        fun stop(context: Context) =
            context.stopService(Intent(context, DockOverlayService::class.java))
    }
}
