package com.orbys.launcherfakets13.util

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import com.orbys.launcherfakets13.R

/**
 * Encapsula el flujo de selección + bind + configuración de un AppWidget del sistema,
 * para poder reutilizarlo desde distintas superficies (capa libre de widgets, Desktop)
 * cada una con su propio [AppWidgetHost].
 */
class WidgetPickerHelper(
    private val context: Context,
    private val appWidgetManager: AppWidgetManager,
    private val host: AppWidgetHost,
    private val bindLauncher: ActivityResultLauncher<Intent>,
    private val configureLauncher: ActivityResultLauncher<Intent>,
    private val onBound: (appWidgetId: Int, info: AppWidgetProviderInfo) -> Unit,
    private val onFailed: () -> Unit = {}
) {
    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    fun launchPicker() {
        val providers = appWidgetManager.installedProviders.sortedBy { it.loadLabel(context.packageManager) }
        if (providers.isEmpty()) return

        val allocatedId = host.allocateAppWidgetId()
        if (allocatedId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val listView = ListView(context)
        listView.divider = null
        listView.dividerHeight = 0

        listView.adapter = object : ArrayAdapter<AppWidgetProviderInfo>(context, 0, providers) {
            override fun getView(pos: Int, recycled: View?, parent: ViewGroup): View {
                val info = getItem(pos)!!
                val dpi = context.resources.displayMetrics.densityDpi

                val row = (recycled as? LinearLayout) ?: LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                row.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                row.removeAllViews()

                val iconIv = ImageView(context)
                iconIv.scaleType = ImageView.ScaleType.CENTER_INSIDE
                val preview = runCatching { info.loadPreviewImage(context, dpi) }.getOrNull()
                    ?: runCatching { info.loadIcon(context, dpi) }.getOrNull()
                if (preview != null) iconIv.setImageDrawable(preview)
                else iconIv.setImageResource(R.drawable.ic_android)
                row.addView(iconIv, LinearLayout.LayoutParams(dpToPx(56), dpToPx(56)).apply {
                    marginEnd = dpToPx(14)
                })

                val widgetLabel = info.loadLabel(context.packageManager)
                val appLabel = runCatching {
                    context.packageManager.getApplicationLabel(
                        context.packageManager.getApplicationInfo(info.provider.packageName, 0)
                    ).toString()
                }.getOrDefault(info.provider.packageName)

                val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                col.addView(TextView(context).apply {
                    text = widgetLabel
                    textSize = 15f
                    setTextColor(context.getColor(R.color.widget_label_primary))
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                })
                col.addView(TextView(context).apply {
                    text = appLabel
                    textSize = 12f
                    setTextColor(context.getColor(R.color.widget_label_secondary))
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                })
                if (widgetLabel.equals(appLabel, ignoreCase = true)) {
                    col.addView(TextView(context).apply {
                        text = info.provider.className.substringAfterLast('.')
                        textSize = 11f
                        setTextColor(context.getColor(R.color.widget_label_tertiary))
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                    })
                }
                row.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                return row
            }
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.widget_picker_title)
            .setView(listView)
            .setNegativeButton(R.string.cancel) { _, _ -> host.deleteAppWidgetId(allocatedId) }
            .create()

        listView.setOnItemClickListener { _, _, pos, _ ->
            dialog.dismiss()
            bindWidget(allocatedId, providers[pos])
        }

        dialog.show()
    }

    private fun bindWidget(allocatedId: Int, provider: AppWidgetProviderInfo) {
        if (appWidgetManager.bindAppWidgetIdIfAllowed(allocatedId, provider.provider)) {
            onWidgetBound(allocatedId)
            return
        }
        pendingWidgetId = allocatedId
        bindLauncher.launch(
            Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            }
        )
    }

    /**
     * Retoma un appWidgetId ya asignado y aceptado fuera de este helper (p. ej. vía
     * AppWidgetManager.requestPinAppWidget(), aceptado por PinItemConfirmActivity), llevándolo
     * por el mismo pipeline de configuración/finalización que sigue un widget elegido con
     * [launchPicker].*/
    fun continueBoundWidget(appWidgetId: Int) = onWidgetBound(appWidgetId)

    /** Debe llamarse desde el callback del [bindLauncher] registrado por el caller. */
    fun onBindResult(resultOk: Boolean) {
        val id = pendingWidgetId
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        if (resultOk) {
            onWidgetBound(id)
        } else {
            host.deleteAppWidgetId(id)
            onFailed()
        }
    }

    private fun onWidgetBound(id: Int) {
        val info = appWidgetManager.getAppWidgetInfo(id)
        if (info?.configure != null) {
            pendingWidgetId = id
            configureLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                }
            )
        } else {
            finalize(id)
        }
    }

    /** Debe llamarse desde el callback del [configureLauncher] registrado por el caller. */
    fun onConfigureResult(resultOk: Boolean) {
        val id = pendingWidgetId
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        if (resultOk) {
            finalize(id)
        } else {
            host.deleteAppWidgetId(id)
            onFailed()
        }
    }

    private fun finalize(id: Int) {
        val info = appWidgetManager.getAppWidgetInfo(id)
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        if (info == null) {
            runCatching { host.deleteAppWidgetId(id) }
            onFailed()
            return
        }
        onBound(id, info)
    }

    private fun dpToPx(dp: Int) = (dp * context.resources.displayMetrics.density).toInt()
}
