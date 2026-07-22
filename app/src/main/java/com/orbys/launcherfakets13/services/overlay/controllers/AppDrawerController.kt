package com.orbys.launcherfakets13.services.overlay.controllers

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.net.toUri
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.orbys.launcherfakets13.data.repository.AppShortcutsRepositoryImpl
import com.orbys.launcherfakets13.data.repository.AppsRepositoryImpl
import com.orbys.launcherfakets13.databinding.ViewMenuAppsBinding
import com.orbys.launcherfakets13.domain.model.AppInfo
import com.orbys.launcherfakets13.domain.usecase.GetAppShortcutsUseCase
import com.orbys.launcherfakets13.domain.usecase.GetInstalledAppsUseCase
import com.orbys.launcherfakets13.domain.usecase.LaunchAppShortcutUseCase
import com.orbys.launcherfakets13.ui.common.AppAdapter
import com.orbys.launcherfakets13.ui.common.AppShortcutsMenu
import com.orbys.launcherfakets13.ui.util.dp
import com.orbys.launcherfakets13.util.AppLauncherHelper

class AppDrawerController(
    context: Context,
    private val getDockView: () -> View?,
    private val onAppLaunched: () -> Unit,
    private val onVisibilityChanged: (Boolean) -> Unit
) : BaseOverlayController(context) {

    private var _binding: ViewMenuAppsBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("AppDrawerController binding is null. Is the view showing?")

    private val getInstalledAppsUseCase = GetInstalledAppsUseCase(AppsRepositoryImpl(context))
    private val appShortcutsRepository = AppShortcutsRepositoryImpl(context)
    private val getAppShortcutsUseCase = GetAppShortcutsUseCase(appShortcutsRepository)
    private val launchAppShortcutUseCase = LaunchAppShortcutUseCase(appShortcutsRepository)

    private var drawerClosedAt = 0L

    fun toggle() {
        if (isVisible()) {
            hideAnimated()
        } else if (System.currentTimeMillis() - drawerClosedAt > 250) {
            show()
        }
    }

    private fun show() {
        if (isVisible()) return

        val b = ViewMenuAppsBinding.inflate(LayoutInflater.from(context))
        _binding = b
        val newView = b.root

        val screenH = context.resources.displayMetrics.heightPixels + 350.dp
        val dockView = getDockView()
        val dockW = dockView?.width?.takeIf { it > 0 } ?: 520.dp
        val dockH = dockView?.height?.takeIf { it > 0 } ?: 100.dp
        val dockMarY = 20.dp
        val gap = 10.dp

        val params = WindowManager.LayoutParams(
            dockW,
            screenH / 2,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            title = "OrbysAppDrawer"
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

        loadApps()
        onVisibilityChanged(true)

        newView.post {
            newView.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    fun hideAnimated() {
        val view = rootView ?: return
        rootView = null // Pre-emptively null out to prevent double calls
        drawerClosedAt = System.currentTimeMillis()
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
    }

    private fun loadApps() {
        val b = _binding ?: return
        b.rvDrawerApps.layoutManager = GridLayoutManager(context, 4)

        val pm = context.packageManager
        val allApps = getInstalledAppsUseCase()

        fun setAdapter(list: List<AppInfo>) {
            _binding?.rvDrawerApps?.adapter = AppAdapter(
                apps = list,
                onAppClick = { app ->
                    val rv = _binding?.rvDrawerApps
                    hideAnimated()
                    onAppLaunched()
                    val componentName = app.componentName
                    rv?.postDelayed({
                        if (componentName != null) {
                            context.getSystemService(LauncherApps::class.java)
                                .startMainActivity(componentName, app.userHandle, null, null)
                        } else {
                            pm.getLaunchIntentForPackage(app.packageName.toString())
                                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ?.let { context.startActivity(it) }
                        }
                    }, 200)
                },
                onSplitClick = { app ->
                    AppLauncherHelper.startAppInFreeform(context, app.packageName.toString())
                    hideAnimated()
                },
                onInfoClick = { app ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:${app.packageName}".toUri()
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    hideAnimated()
                },
                onUninstallClick = { app ->
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = "package:${app.packageName}".toUri()
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    hideAnimated()
                },
                onShortcutsClick = { app, anchor ->
                    val shortcuts = getAppShortcutsUseCase(app.packageName.toString(), app.userHandle)
                    AppShortcutsMenu.show(anchor, shortcuts) { shortcut ->
                        launchAppShortcutUseCase(shortcut)
                        hideAnimated()
                    }
                }
            )
        }

        setAdapter(allApps)

        b.etDrawerSearch.doOnTextChanged { text, _, _, _ ->
            val q = text?.toString()?.lowercase().orEmpty()
            setAdapter(if (q.isEmpty()) allApps else allApps.filter { it.label.toString().lowercase().contains(q) })
        }
    }

    override fun removeView() {
        hideAnimated()
    }
}
