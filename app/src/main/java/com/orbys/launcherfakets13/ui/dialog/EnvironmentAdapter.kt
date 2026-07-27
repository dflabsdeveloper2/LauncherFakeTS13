package com.orbys.launcherfakets13.ui.dialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.domain.model.Environment
import com.orbys.launcherfakets13.ui.util.EnvironmentMapper

/**
 * Adaptador para mostrar las opciones de entorno en el diálogo de selección.
 */
class EnvironmentAdapter(
    private val items: List<Environment>,
    initialEnvironment: Environment? = null,
    private val onItemClick: (Environment) -> Unit
) : RecyclerView.Adapter<EnvironmentAdapter.ViewHolder>() {

    private var selectedPosition = initialEnvironment?.let { env ->
        items.indexOfFirst { it == env }
    }?.takeIf { it != -1 } ?: 0

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.card)
        val icon: ImageView = itemView.findViewById(R.id.ivIcon)
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val category: TextView = itemView.findViewById(R.id.tvCategory)
        val footer: TextView = itemView.findViewById(R.id.tvFooter)
        val badge: TextView = itemView.findViewById(R.id.tvBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_environment_card, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val env = items[position]
        val ctx = holder.itemView.context
        val isSelected = position == selectedPosition

        // Resolución de recursos mediante el Mapper de UI
        holder.title.text = ctx.getString(EnvironmentMapper.getTitleRes(env))
        holder.category.text = ctx.getString(EnvironmentMapper.getCategoryRes(env))
        holder.footer.text = ctx.getString(EnvironmentMapper.getFooterRes(env))
        holder.icon.setImageResource(EnvironmentMapper.getIconRes(env))

        if (isSelected) {
            holder.card.setCardBackgroundColor(ctx.getColor(R.color.environment_selected_bg))
            holder.title.setTextColor(ctx.getColor(R.color.white))
            holder.category.setTextColor(ctx.getColor(R.color.white_65))
            holder.footer.setTextColor(ctx.getColor(R.color.white_65))
            holder.icon.background.setTint(ctx.getColor(R.color.environment_badge_selected_bg))
            holder.badge.visibility = View.VISIBLE
            // Quitar el filtro de color si el icono tiene sus propios colores (Google/Microsoft)
            if (env == Environment.OFFICE || env == Environment.GOOGLE) {
                holder.icon.clearColorFilter()
            } else {
                holder.icon.setColorFilter(ctx.getColor(R.color.white))
            }
        } else {
            holder.card.setCardBackgroundColor(ctx.getColor(R.color.environment_unselected_bg))
            holder.title.setTextColor(ctx.getColor(R.color.environment_unselected_title))
            holder.category.setTextColor(ctx.getColor(R.color.dock_text_inactive))
            holder.footer.setTextColor(ctx.getColor(R.color.dock_text_inactive))
            holder.icon.background.setTint(ctx.getColor(R.color.white))
            holder.badge.visibility = View.GONE
            
            if (env == Environment.OFFICE || env == Environment.GOOGLE) {
                holder.icon.clearColorFilter()
            } else {
                holder.icon.setColorFilter(ctx.getColor(R.color.environment_icon_unselected_tint))
            }
        }

        holder.card.setOnClickListener {
            val prev = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)
            onItemClick(env)
        }
    }

    override fun getItemCount() = items.size
}
