package com.orbys.launcherfakets13.services.overlay.controllers

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.home.MainActivity
import com.orbys.launcherfakets13.util.SystemActionHelper
import com.orbys.launcherfakets13.ui.util.dp
import kotlin.math.abs

class SidebarController(
    context: Context,
    private val side: Side,
    private val onTuneClick: (Int, Int, Int, Boolean) -> Unit,
    private val onToggleDock: () -> Unit,
    private val onExpandSync: () -> Unit,
    private val onCollapseSync: () -> Unit,
    private val isDockExpanded: () -> Boolean
) : BaseOverlayController(context) {

    enum class Side { LEFT, RIGHT }

    var sidebarExpanded = false
        private set

    fun show() {
        if (isVisible()) return

        val layoutId = if (side == Side.LEFT) R.layout.view_sidebar_left_overlay else R.layout.view_sidebar_right_overlay
        val newView = LayoutInflater.from(context).inflate(layoutId, null)

        val screenH = context.resources.displayMetrics.heightPixels
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            title = if (side == Side.LEFT) "OrbysSidebarLeft" else "OrbysSidebarRight"
            gravity = (if (side == Side.LEFT) Gravity.START else Gravity.END) or Gravity.TOP
            x = 0
            y = screenH / 2 - 55.dp / 2
        }
        
        setupInteractions(newView)
        addViewSafely(newView, params)
    }

    private fun setupInteractions(v: View) {
        val panel = v.findViewById<View>(R.id.sidebar_panel)
        val handle = v.findViewById<View>(R.id.sidebar_handle)

        v.findViewById<View>(R.id.sidebar_btn_back).setOnClickListener {
            SystemActionHelper.performBack(context)
        }

        v.findViewById<View>(R.id.sidebar_btn_home).setOnClickListener {
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        }

        /*v.findViewById<View>(R.id.sidebar_btn_recents).setOnClickListener {
            DockOverlayService.toggleRecents()
        }*/

        v.findViewById<View>(R.id.sidebar_btn_tune).setOnClickListener {
            val sw = panel.width.takeIf { it > 0 } ?: 40.dp
            val sy = layoutParams?.y ?: 0
            val sh = (if (sidebarExpanded) panel.height else handle.height).takeIf { it > 0 } ?: 55.dp
            onTuneClick(sw, sy, sh, side == Side.RIGHT)
        }

        syncDockIcon()
        v.findViewById<View>(R.id.sidebar_btn_expand_dock).setOnClickListener {
            Log.d("SidebarController", "sidebar_btn_expand_dock clicked (side=$side)")
            onToggleDock()
            // We don't need syncDockIcon() here because onToggleDock() triggers
            // DockController which calls syncSidebarsDockIcon() via callback.
        }

        v.findViewById<View>(R.id.sidebar_btn_collapse).setOnClickListener { 
            toggleSidebar()
        }

        setupDrag(handle)
    }

    private fun setupDrag(handle: View) {
        var downRawY = 0f
        var downParamY = 0
        var hasMoved = false

        handle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawY = event.rawY
                    downParamY = layoutParams?.y ?: 0
                    hasMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val delta = (event.rawY - downRawY).toInt()
                    if (abs(delta) > 10) hasMoved = true
                    if (hasMoved) {
                        val screenH = context.resources.displayMetrics.heightPixels
                        val viewH = rootView?.height ?: 200
                        layoutParams?.y = (downParamY + delta).coerceIn(0, screenH - viewH)
                        updateViewSafely()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved) toggleSidebar()
                    true
                }
                else -> false
            }
        }
    }

    fun syncDockIcon() {
        val res = if (isDockExpanded()) R.drawable.fl34 else R.drawable.fl32
        rootView?.findViewById<ImageView>(R.id.sidebar_btn_expand_dock)?.setImageResource(res)
    }

    fun expand() {
        if (sidebarExpanded) return
        val v = rootView ?: return
        val panel = v.findViewById<View>(R.id.sidebar_panel)
        val handle = v.findViewById<View>(R.id.sidebar_handle)

        sidebarExpanded = true
        val handleH = 55.dp
        val handleCenter = (layoutParams?.y ?: 0) + handleH / 2
        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val panelH = panel.measuredHeight
        val screenH = context.resources.displayMetrics.heightPixels

        layoutParams?.y = (handleCenter - panelH / 2).coerceIn(0, screenH - panelH)
        handle.visibility = View.GONE
        panel.alpha = 0f
        panel.translationX = (if (side == Side.LEFT) -30.dp else 30.dp).toFloat()
        panel.visibility = View.VISIBLE
        updateViewSafely()

        panel.animate()
            .alpha(1f).translationX(0f)
            .setDuration(200).setInterpolator(DecelerateInterpolator()).start()
    }

    fun collapse() {
        if (!sidebarExpanded) return
        val v = rootView ?: return
        val panel = v.findViewById<View>(R.id.sidebar_panel)
        val handle = v.findViewById<View>(R.id.sidebar_handle)

        sidebarExpanded = false
        val panelH = panel.height.takeIf { it > 0 } ?: panel.run {
            measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED); measuredHeight
        }
        val handleH = 55.dp
        val panelCenter = (layoutParams?.y ?: 0) + panelH / 2
        val screenH = context.resources.displayMetrics.heightPixels
        val targetY = (panelCenter - handleH / 2).coerceIn(0, screenH - handleH)

        // 1. Ocultar el panel con fade-out rápido
        panel.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                panel.visibility = View.GONE
                panel.alpha = 1f // restaurar para próxima expansión

                // 2. Posicionar el handle fuera de pantalla (lado izquierdo) antes de mostrarlo
                layoutParams?.y = targetY
                handle.translationX = -handle.width.toFloat().takeIf { it > 0 }!! ?: -200f
                handle.alpha = 0f
                handle.visibility = View.VISIBLE
                rootView?.post { updateViewSafely() }

                // 3. Tras 0.5 segundo, animar la entrada del handle
                handle.postDelayed({
                    handle.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(350)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }, 300L)
            }
            .start()
    }
    private fun toggleSidebar() {
        if (!sidebarExpanded) {
            expand()
            onExpandSync()
        } else {
            collapse()
            onCollapseSync()
        }
    }
}
