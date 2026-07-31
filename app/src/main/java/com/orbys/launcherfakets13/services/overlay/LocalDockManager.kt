package com.orbys.launcherfakets13.services.overlay

import android.content.Context
import android.content.Intent
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.data.repository.AppsRepositoryImpl
import com.orbys.launcherfakets13.services.overlay.controllers.AppDrawerController
import com.orbys.launcherfakets13.services.overlay.controllers.ControlPanelController
import com.orbys.launcherfakets13.services.overlay.controllers.DockController
import com.orbys.launcherfakets13.services.overlay.controllers.RecentsController
import com.orbys.launcherfakets13.ui.dialog.RemoteModeActivity
import com.orbys.launcherfakets13.ui.home.MainActivity

class LocalDockManager(
    private val context: Context,
    private val container: ViewGroup
) {
    private val themedCtx = ContextThemeWrapper(context, R.style.Theme_LauncherTs13)

    private lateinit var dockController: DockController
    private lateinit var drawerController: AppDrawerController
    private lateinit var controlPanelController: ControlPanelController
    private lateinit var recentsController: RecentsController

    init {
        // Precalienta la caché de apps instaladas en segundo plano
        AppsRepositoryImpl.prefetch(context.applicationContext)
        initControllers()
        dockController.show()
        
        instance = this
    }

    private fun initControllers() {
        dockController = DockController(
            themedCtx,
            container,
            onAppsTabClick = { drawerController.toggle() },
            onFilesTabClick = { openFileManager() },
            onPizarraTabClick = { openPizarra() },
            onBrowserTabClick = { openBrowser() },
            onSettingsTabClick = { openSettings() }
        )

        drawerController = AppDrawerController(
            themedCtx,
            container,
            getDockView = { dockController.rootView },
            onAppLaunched = { dockController.collapse() },
            onVisibilityChanged = { visible ->
                if (visible) {
                    recentsController.hideAnimated()
                    dockController.selectTab(R.id.tab_apps)
                } else {
                    dockController.clearTabSelection(R.id.tab_apps)
                }
            }
        )

        controlPanelController = ControlPanelController(themedCtx, container)

        recentsController = RecentsController(
            themedCtx,
            container,
            getDockView = { dockController.rootView },
            onVisibilityChanged = { visible ->
                if (visible) {
                    drawerController.hideAnimated()
                    dockController.selectTab(R.id.tab_recents)
                } else {
                    dockController.clearTabSelection(R.id.tab_recents)
                }
            }
        )
    }

    fun onDestroy() {
        if (instance === this) instance = null
        dockController.removeView()
        drawerController.removeView()
        controlPanelController.removeView()
        recentsController.removeView()
    }

    // ── Helper app launchers ──────────────────────────────────────────────────

    private fun openFileManager() = startRemoteModeActivity()
    private fun openPizarra() = startRemoteModeActivity()
    private fun openBrowser() = startRemoteModeActivity()
    private fun openSettings() = startRemoteModeActivity()

    private fun hideActivePanels() {
        if (drawerController.isVisible()) drawerController.hideAnimated()
        if (recentsController.isVisible()) recentsController.hideAnimated()
    }

    private fun startRemoteModeActivity() {
        hideActivePanels()
        val intent = Intent(context, RemoteModeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        private var instance: LocalDockManager? = null

        fun minimize() = instance?.dockController?.collapse()
        fun toggleDock() = instance?.dockController?.toggle()
        fun showDock() = instance?.dockController?.expand()
        fun isDockExpanded() = instance?.dockController?.isExpanded ?: true
        
        fun toggleVolBright(sidebarW: Int, sidebarY: Int, sidebarH: Int = 0, fromRight: Boolean = false) {
            instance?.controlPanelController?.toggle(sidebarW, sidebarY, sidebarH, fromRight)
        }

        fun hideVolBright() = instance?.controlPanelController?.removeView()
        fun toggleRecents() = instance?.recentsController?.toggle()
        fun clearDockSelection() = instance?.dockController?.clearSelection()

        fun hideActivePanels() = instance?.hideActivePanels()

        fun setDockVisibility(visible: Boolean) {
            instance?.dockController?.rootView?.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }
}
