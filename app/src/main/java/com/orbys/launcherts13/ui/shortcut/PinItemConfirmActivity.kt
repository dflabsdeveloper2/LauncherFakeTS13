package com.orbys.launcherts13.ui.shortcut

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.pm.LauncherApps
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherts13.R
import com.orbys.launcherts13.util.DesktopWidgetHost
import com.orbys.launcherts13.util.PendingPinnedWidgetsStore

/**
 * Maneja LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT y ACTION_CONFIRM_PIN_APPWIDGET (CDD
 * 3.8.1/H-SR-1): cuando una app de terceros llama a ShortcutManager.requestPinShortcut()
 * o AppWidgetManager.requestPinAppWidget(), el sistema lanza esta Activity para pedir
 * confirmación al usuario antes de anclar el shortcut o el widget.
 *
 * El shortcut aceptado queda registrado como "pinned" en ShortcutManager y aparece en el
 * menú de shortcuts de la app (ver AppShortcutsMenu). El widget aceptado se aloja en el
 * mismo AppWidgetHost que usa el Desktop ([DesktopWidgetHost]) y su id queda en
 * [PendingPinnedWidgetsStore] hasta que DesktopFragment lo coloque en una celda libre del
 * grid, ya que esta Activity no conoce la geometría del grid (depende del tamaño de
 * pantalla medido en tiempo de layout).
 */
class PinItemConfirmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launcherApps = getSystemService(LauncherApps::class.java)
        val request = runCatching { launcherApps.getPinItemRequest(intent) }.getOrNull()

        if (request == null || !request.isValid) {
            finish()
            return
        }

        when (request.requestType) {
            LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT -> confirmShortcut(request)
            LauncherApps.PinItemRequest.REQUEST_TYPE_APPWIDGET -> confirmWidget(request)
            else -> finish()
        }
    }

    private fun confirmShortcut(request: LauncherApps.PinItemRequest) {
        val label = request.shortcutInfo?.shortLabel
            ?: request.shortcutInfo?.longLabel
            ?: getString(R.string.pin_shortcut_default_label)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pin_shortcut_title)
            .setMessage(getString(R.string.pin_shortcut_message, label))
            .setPositiveButton(R.string.pin_shortcut_add) { _, _ ->
                val accepted = runCatching { request.accept() }.getOrDefault(false)
                Toast.makeText(
                    this,
                    if (accepted) R.string.pin_shortcut_added else R.string.pin_shortcut_failed,
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun confirmWidget(request: LauncherApps.PinItemRequest) {
        val info = request.getAppWidgetProviderInfo(this)
        if (info == null) {
            finish()
            return
        }
        val label = info.loadLabel(packageManager)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pin_widget_title)
            .setMessage(getString(R.string.pin_widget_message, label))
            .setPositiveButton(R.string.pin_shortcut_add) { _, _ ->
                acceptWidget(request)
                finish()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun acceptWidget(request: LauncherApps.PinItemRequest) {
        // Host "sin vista": solo para reservar el id bajo el mismo hostId que usa
        // DesktopFragment. La vista real se crea allí cuando coloca el widget.
        val host = AppWidgetHost(applicationContext, DesktopWidgetHost.ID)
        val appWidgetId = host.allocateAppWidgetId()

        val accepted = runCatching {
            request.accept(Bundle().apply { putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) })
        }.getOrDefault(false)

        if (accepted) {
            PendingPinnedWidgetsStore.add(applicationContext, appWidgetId)
            Toast.makeText(this, R.string.pin_widget_added, Toast.LENGTH_SHORT).show()
        } else {
            runCatching { host.deleteAppWidgetId(appWidgetId) }
            Toast.makeText(this, R.string.pin_widget_failed, Toast.LENGTH_SHORT).show()
        }
    }
}