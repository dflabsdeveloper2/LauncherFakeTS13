package com.orbys.launcherfakets13.services.overlay.controllers

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.databinding.ViewDockOverlayBinding
import com.orbys.launcherfakets13.services.overlay.DockOverlayService
import com.orbys.launcherfakets13.ui.home.MainActivity
import com.orbys.launcherfakets13.ui.util.dp
import com.orbys.launcherfakets13.util.SystemActionHelper

class DockController(
    context: Context,
    private val onAppsTabClick: () -> Unit,
    private val onFilesTabClick: () -> Unit,
    private val onPizarraTabClick: () -> Unit,
    private val onBrowserTabClick: () -> Unit,
    private val onSettingsTabClick: () -> Unit,
    private val onToggleDock: (Boolean) -> Unit
) : BaseOverlayController(context) {

    private var _binding: ViewDockOverlayBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("DockController binding is null. Is the view showing?")
    
    var isExpanded = true
        private set

    private var activeTabId = -1

    fun show() {
        if (isVisible()) return

        _binding = ViewDockOverlayBinding.inflate(LayoutInflater.from(context))
        val newView = binding.root

        val dockMargin = 20.dp
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            title = "OrbysDock"
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dockMargin
        }

        setupInteractions()
        updateTabUI(activeTabId)
        
        addViewSafely(newView, params)
    }

    private fun setupInteractions() {
        binding.dockToggleHandle.setOnClickListener { expand() }

        binding.tabBack.setOnClickListener {
            SystemActionHelper.performBack(context)
        }

        binding.tabHome.setOnClickListener {
            selectTab(R.id.tab_home)
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        }

        binding.tabRecents.setOnClickListener {
            selectTab(R.id.tab_recents)
            DockOverlayService.toggleRecents()
        }

        binding.tabApps.setOnClickListener {
            selectTab(R.id.tab_apps)
            onAppsTabClick()
        }

        binding.tabFiles.setOnClickListener {
            selectTab(R.id.tab_files)
            onFilesTabClick()
        }

        binding.tabPizarra.setOnClickListener {
            selectTab(R.id.tab_pizarra)
            onPizarraTabClick()
        }

        binding.tabBrowser.setOnClickListener {
            selectTab(R.id.tab_browser)
            onBrowserTabClick()
        }

        binding.tabSettings.setOnClickListener {
            selectTab(R.id.tab_settings)
            onSettingsTabClick()
        }
    }

    fun toggle() {
        Log.d("DockController", "toggle() called, isExpanded=$isExpanded, hasBinding=${_binding != null}")
        if (isExpanded) collapse() else expand()
    }

    fun expand() {
        // Self-heal: the view can be gone (e.g. DockOverlayService.refreshUI() removed it
        // without a matching re-show) while isExpanded is left stale from before. Recreating
        // it here already leaves the dock expanded (its default state), so there's nothing left to do.
        if (_binding == null) {
            Log.w("DockController", "expand(): binding missing, recreating dock view")
            show()
            return
        }
        if (isExpanded) {
            Log.d("DockController", "expand() ignored, already expanded")
            return
        }
        val b = _binding ?: return
        isExpanded = true
        b.dockToggleHandle.visibility = View.GONE
        b.dockPanel.visibility = View.VISIBLE
        layoutParams?.y = 20.dp
        updateViewSafely()
        onToggleDock(true)
    }

    fun collapse() {
        if (_binding == null) {
            Log.w("DockController", "collapse(): binding missing, recreating dock view then collapsing")
            show()
            collapse()
            return
        }
        if (!isExpanded) {
            Log.d("DockController", "collapse() ignored, already collapsed")
            return
        }
        val b = _binding ?: return
        isExpanded = false
        b.dockPanel.visibility = View.GONE
        b.dockToggleHandle.visibility = View.VISIBLE
        layoutParams?.y = 0
        updateViewSafely()
        onToggleDock(false)
    }

    fun selectTab(tabId: Int) {
        activeTabId = tabId
        updateTabUI(tabId)
    }

    private fun updateTabUI(activeId: Int) {
        val b = _binding ?: return
        val activeColor = ContextCompat.getColor(context, R.color.white)
        val inactiveColor = ContextCompat.getColor(context, R.color.dock_text_inactive)

        // Reset solo apps y recents
        listOf(
            Triple(b.tabApps, b.icTabApps, b.lblTabApps),
            Triple(b.tabRecents, b.icTabRecents, b.lblTabRecents)
        ).forEach { (tab, icon, label) ->
            tab.background = null
            icon.imageTintList = ColorStateList.valueOf(inactiveColor)
            label.setTextColor(inactiveColor)
        }

        // Activar solo si es apps o recents
        val (tab, icon, label) = when (activeId) {
            R.id.tab_apps -> Triple(b.tabApps, b.icTabApps, b.lblTabApps)
            R.id.tab_recents -> Triple(b.tabRecents, b.icTabRecents, b.lblTabRecents)
            else -> return
        }
        tab.background = ContextCompat.getDrawable(context, R.drawable.bg_dock_tab_active)
        icon.imageTintList = ColorStateList.valueOf(activeColor)
        label.setTextColor(activeColor)
    }

    fun clearTabSelection(tabId: Int) {
        if (activeTabId == tabId) clearSelection()
    }

    fun clearSelection() {
        activeTabId = -1
        updateTabUI(-1)
    }

    override fun removeView() {
        super.removeView()
        _binding = null
    }
}
