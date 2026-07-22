package com.orbys.launcherfakets13.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orbys.launcherfakets13.R

/**
 * Carpeta de apps de Google (CDD/EDLA): icono fijo en el Home que muestra un listado de apps
 * de Google, filtrando las que no estén instaladas en vez de mostrarlas rotas.
 */
class GoogleAppsFolderDialog : DialogFragment() {

    private data class GoogleApp(val packageName: String, val label: String)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val pm = requireContext().packageManager
        val apps = listOf(
            GoogleApp("com.google.android.gm", getString(R.string.google_app_gmail)),
            GoogleApp("com.google.android.apps.maps", getString(R.string.google_app_maps)),
            GoogleApp("com.google.android.youtube", getString(R.string.google_app_youtube)),
            GoogleApp("com.google.android.apps.photos", getString(R.string.google_app_photos)),
            GoogleApp("com.google.android.apps.docs", getString(R.string.google_app_drive)),
            GoogleApp("com.google.android.apps.tachyon", getString(R.string.google_app_meet))
        ).filter { pm.getLaunchIntentForPackage(it.packageName) != null }

        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_google_apps_folder, null)

        view.findViewById<RecyclerView>(R.id.rv_google_apps).apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = GoogleAppsAdapter(apps) { app ->
                pm.getLaunchIntentForPackage(app.packageName)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(it)
                }
                dismiss()
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()
            .also { dialog -> dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded) }
    }

    private inner class GoogleAppsAdapter(
        private val apps: List<GoogleApp>,
        private val onClick: (GoogleApp) -> Unit
    ) : RecyclerView.Adapter<GoogleAppsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.iv_result_icon)
            val label: TextView = view.findViewById(R.id.tv_result_title)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_grid, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            val icon = runCatching { requireContext().packageManager.getApplicationIcon(app.packageName) }.getOrNull()
            if (icon != null) holder.icon.setImageDrawable(icon) else holder.icon.setImageResource(R.drawable.ic_android)
            holder.label.text = app.label
            holder.itemView.setOnClickListener { onClick(app) }
        }

        override fun getItemCount() = apps.size
    }

    companion object {
        fun newInstance() = GoogleAppsFolderDialog()
    }
}
