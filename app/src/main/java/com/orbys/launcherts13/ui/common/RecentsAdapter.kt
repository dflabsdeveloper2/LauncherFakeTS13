package com.orbys.launcherts13.ui.common

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.orbys.launcherts13.databinding.ItemRecentAppBinding
import com.orbys.launcherts13.domain.model.RecentAppInfo

/**
 * Adapter for the recent applications list.
 *
 * @property apps List of recent applications to display.
 * @property onAppClick Callback when an application is clicked.
 * @property onDeleteClick Callback when the delete icon is clicked for an application.
 */
class RecentsAdapter(
    private var apps: List<RecentAppInfo>,
    private val onAppClick: (RecentAppInfo) -> Unit,
    private val onDeleteClick: (RecentAppInfo) -> Unit
) : RecyclerView.Adapter<RecentsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRecentAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]

        val bitmap = BitmapFactory.decodeFile(app.imagePath)
        holder.binding.ivAppIcon.setImageBitmap(bitmap)

        holder.binding.root.setOnClickListener { onAppClick(app) }
        holder.binding.ivDeleteApp.setOnClickListener { onDeleteClick(app) }
    }

    override fun getItemCount(): Int = apps.size

    fun updateApps(newApps: List<RecentAppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
