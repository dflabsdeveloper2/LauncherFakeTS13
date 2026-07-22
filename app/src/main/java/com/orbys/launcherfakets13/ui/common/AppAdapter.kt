package com.orbys.launcherfakets13.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.domain.model.AppInfo
import com.orbys.launcherfakets13.util.NotificationBadgeStore

class AppAdapter(
    private val apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onSplitClick: (AppInfo) -> Unit = {},
    private val onInfoClick: (AppInfo) -> Unit = {},
    private val onUninstallClick: (AppInfo) -> Unit = {},
    private val onShortcutsClick: (AppInfo, View) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    private var selectedPosition: Int = -1

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iv_app_icon)
        val name: TextView = view.findViewById(R.id.tv_app_name)
        val badge: TextView = view.findViewById(R.id.tv_notification_badge)
        val actionsLayout: LinearLayout = view.findViewById(R.id.ll_app_actions)
        val btnSplit: View = view.findViewById(R.id.btn_action_split)
        val btnInfo: View = view.findViewById(R.id.btn_action_info)
        val btnUninstall: View = view.findViewById(R.id.btn_action_uninstall)
        val btnShortcuts: View = view.findViewById(R.id.btn_action_shortcuts)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.label

        val notificationCount = NotificationBadgeStore.countFor(app.packageName.toString())
        if (notificationCount > 0) {
            holder.badge.visibility = View.VISIBLE
            holder.badge.text = if (notificationCount > 99) "99+" else notificationCount.toString()
        } else {
            holder.badge.visibility = View.GONE
        }

        val isSelected = selectedPosition == position
        
        if (isSelected) {
            holder.actionsLayout.visibility = View.VISIBLE
            val shake = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.shake)
            holder.icon.startAnimation(shake)
        } else {
            holder.actionsLayout.visibility = View.GONE
            holder.icon.clearAnimation()
        }

        holder.itemView.setOnClickListener {
            if (selectedPosition != -1) {
                val oldPos = selectedPosition
                selectedPosition = -1
                notifyItemChanged(oldPos)
            } else {
                onAppClick(app)
            }
        }

        holder.itemView.setOnLongClickListener {
            val oldPos = selectedPosition
            selectedPosition = position
            if (oldPos != -1) notifyItemChanged(oldPos)
            notifyItemChanged(selectedPosition)
            true
        }

        holder.btnSplit.setOnClickListener {
            onSplitClick(app)
            hideActions()
        }
        holder.btnInfo.setOnClickListener {
            onInfoClick(app)
            hideActions()
        }
        holder.btnUninstall.setOnClickListener {
            onUninstallClick(app)
            hideActions()
        }
        holder.btnShortcuts.setOnClickListener {
            onShortcutsClick(app, holder.btnShortcuts)
            hideActions()
        }
    }

    private fun hideActions() {
        if (selectedPosition != -1) {
            val oldPos = selectedPosition
            selectedPosition = -1
            notifyItemChanged(oldPos)
        }
    }

    override fun getItemCount() = apps.size
}
