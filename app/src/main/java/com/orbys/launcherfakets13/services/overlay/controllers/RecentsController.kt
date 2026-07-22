package com.orbys.launcherfakets13.services.overlay.controllers

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.SeekBar
import androidx.recyclerview.widget.GridLayoutManager
import com.orbys.launcherfakets13.databinding.ViewRecentsOverlayBinding
import com.orbys.launcherfakets13.services.overlay.DockOverlayService
import com.orbys.launcherfakets13.ui.common.RecentsAdapter
import com.orbys.launcherfakets13.ui.util.dp
import com.orbys.launcherfakets13.util.RecentsHelper

/**
 * Controller for the custom Recents overlay.
 */
class RecentsController(
    context: Context,
    private val getDockView: () -> View?,
    private val onVisibilityChanged: (Boolean) -> Unit
) : BaseOverlayController(context) {

    private var _binding: ViewRecentsOverlayBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("RecentsController binding is null. Is the view showing?")
    
    private var recentsClosedAt = 0L
    private var adapter: RecentsAdapter? = null

    fun toggle() {
        if (isVisible()) {
            hideAnimated()
        } else if (System.currentTimeMillis() - recentsClosedAt > 250) {
            show()
        }
    }

    private fun show() {
        if (isVisible()) return

        _binding = ViewRecentsOverlayBinding.inflate(LayoutInflater.from(context))
        val newView = binding.root

        val dockView = getDockView()
        val dockH = dockView?.height?.takeIf { it > 0 } ?: 100.dp
        val dockMarY = 20.dp
        val gap = 10.dp

        val params = WindowManager.LayoutParams(
            500.dp,
            300.dp,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            title = "OrbysRecents"
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dockH + dockMarY + gap
        }

        newView.alpha = 0f
        newView.translationY = 300f

        addViewSafely(newView, params)

        newView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) hideAnimated()
            false
        }

        setupRecyclerView()
        setupClearAllSlider()
        loadRecentApps()
        onVisibilityChanged(true)

        binding.root.post {
            binding.root.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    fun hideAnimated() {
        val view = rootView ?: return
        rootView = null 
        recentsClosedAt = System.currentTimeMillis()
        onVisibilityChanged(false)

        view.animate().cancel()
        view.animate()
            .translationY(300f)
            .alpha(0f)
            .setDuration(220)
            .setInterpolator(AccelerateInterpolator())
            .start()

        view.postDelayed({
            if (view.isAttachedToWindow) {
                windowManager.removeViewImmediate(view)
            }
        }, 240)
        _binding = null

        DockOverlayService.clearDockSelection()
    }

    private fun setupRecyclerView() {
        binding.rvRecentsApps.layoutManager = GridLayoutManager(context, 5)
        adapter = RecentsAdapter(
            apps = emptyList(),
            onAppClick = { app ->
                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                intent?.let { context.startActivity(it) }
                hideAnimated()
            },
            onDeleteClick = { app ->
                RecentsHelper.killApk(app.taskId) { success ->
                    if (success) {
                        loadRecentApps()
                    }
                }
            }
        )
        binding.rvRecentsApps.adapter = adapter
    }

    private fun setupClearAllSlider() {
        binding.sbClearAll.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress >= 95) {
                    clearAllApps()
                    seekBar?.progress = 0
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null && seekBar.progress < 95) {
                    seekBar.progress = 0
                }
            }
        })
    }

    private fun loadRecentApps() {
        val apps = RecentsHelper.getRecentBackgroundApps(context) ?: emptyList()
        adapter?.updateApps(apps)
    }

    private fun clearAllApps() {
        val apps = RecentsHelper.getRecentBackgroundApps(context) ?: emptyList()
        var remaining = apps.size
        if (remaining == 0) {
            hideAnimated()
            return
        }
        
        apps.forEach { app ->
            RecentsHelper.killApk(app.taskId) {
                remaining--
                if (remaining <= 0) {
                    hideAnimated()
                }
            }
        }
    }

    override fun removeView() {
        hideAnimated()
    }
}
