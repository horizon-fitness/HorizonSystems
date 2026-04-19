package com.example.horizonsystems

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Notification

class NotificationAdapter(
    private var notifications: MutableList<Notification>,
    private val onDelete: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivNotifType)
        val title: TextView = view.findViewById(R.id.tvNotifTitle)
        val message: TextView = view.findViewById(R.id.tvNotifMessage)
        val time: TextView = view.findViewById(R.id.tvNotifTime)
        val unreadDot: View = view.findViewById(R.id.unreadDot)
        val btnDelete: View = view.findViewById(R.id.btnDeleteNotif)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notif = notifications[position]
        holder.title.text = notif.title
        holder.message.text = notif.message
        holder.time.text = notif.time_ago ?: "just now"
        
        // Handle unread status
        holder.unreadDot.visibility = if (notif.is_read) View.GONE else View.VISIBLE
        
        // Handle icons based on type
        when (notif.notification_type) {
            "membership" -> holder.icon.setImageResource(R.drawable.ic_membership)
            "booking" -> holder.icon.setImageResource(R.drawable.ic_booking)
            else -> holder.icon.setImageResource(R.drawable.ic_notifications)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(notif)
        }

        // Apply Gym Branding
        val themeColorStr = com.example.horizonsystems.utils.GymManager.getThemeColor(holder.itemView.context)
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                holder.time.setTextColor(themeColor)
                holder.icon.imageTintList = android.content.res.ColorStateList.valueOf(themeColor)
                holder.unreadDot.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor)
            } catch (e: Exception) {}
        }
    }

    override fun getItemCount() = notifications.size

    fun updateData(newNotifs: List<Notification>) {
        this.notifications = newNotifs.toMutableList()
        notifyDataSetChanged()
    }
}
