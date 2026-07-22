package com.orbys.launcherfakets13.ui.drawer

import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.domain.usecase.GetAppShortcutsUseCase
import com.orbys.launcherfakets13.domain.usecase.GetInstalledAppsUseCase
import com.orbys.launcherfakets13.domain.usecase.LaunchAppShortcutUseCase
import com.orbys.launcherfakets13.ui.common.AppAdapter
import com.orbys.launcherfakets13.ui.common.AppShortcutsMenu
import com.orbys.launcherfakets13.util.AppLauncherHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppMenuFragment : BottomSheetDialogFragment() {

    @Inject lateinit var getInstalledAppsUseCase: GetInstalledAppsUseCase
    @Inject lateinit var getAppShortcutsUseCase: GetAppShortcutsUseCase
    @Inject lateinit var launchAppShortcutUseCase: LaunchAppShortcutUseCase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_app_drawer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.rv_drawer_apps)
        recycler.layoutManager = GridLayoutManager(requireContext(), 4)

        val apps = getInstalledAppsUseCase()

        recycler.adapter = AppAdapter(
            apps = apps,
            onAppClick = { app ->
                dismiss()
                val componentName = app.componentName
                if (componentName != null) {
                    requireContext().getSystemService(LauncherApps::class.java)
                        .startMainActivity(componentName, app.userHandle, null, null)
                } else {
                    requireContext().packageManager.getLaunchIntentForPackage(app.packageName.toString())
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ?.let { startActivity(it) }
                }
            },
            onSplitClick = { app ->
                AppLauncherHelper.startAppInFreeform(requireContext(), app.packageName.toString())
                dismiss()
            },
            onInfoClick = { app ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${app.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                dismiss()
            },
            onUninstallClick = { app ->
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = "package:${app.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                dismiss()
            },
            onShortcutsClick = { app, anchor ->
                val shortcuts = getAppShortcutsUseCase(app.packageName.toString(), app.userHandle)
                AppShortcutsMenu.show(anchor, shortcuts) { shortcut ->
                    launchAppShortcutUseCase(shortcut)
                    dismiss()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
    }

    override fun getTheme() = R.style.AppDrawerTheme
}
