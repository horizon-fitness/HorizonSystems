package com.example.horizonsystems

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.TrainingLog
import com.example.horizonsystems.utils.GymManager
import java.text.SimpleDateFormat
import java.util.*

class TrainingLogAdapter(
    private var logs: List<TrainingLog>,
    private val onCancelClick: (TrainingLog) -> Unit = {}
) : RecyclerView.Adapter<TrainingLogAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val month: TextView = view.findViewById(R.id.tvLogMonth)
        val day: TextView = view.findViewById(R.id.tvLogDay)
        val time: TextView = view.findViewById(R.id.logTime)
        val service: TextView = view.findViewById(R.id.logService)
        val trainer: TextView = view.findViewById(R.id.logTrainer)
        val status: TextView = view.findViewById(R.id.logStatus)
        val btnCancel: TextView = view.findViewById(R.id.btnCancelBooking)
        val cardDateBlock: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardDateBlock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs.getOrNull(position) ?: return
        val context = holder.itemView.context
        
        // --- 1. Date Parsing (Month/Day) ---
        try {
            val dateStr = log.date ?: "2024-01-01"
            val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateObj = inputSdf.parse(dateStr)
            
            if (dateObj != null) {
                holder.month.text = SimpleDateFormat("MMM", Locale.US).format(dateObj).uppercase()
                holder.day.text = SimpleDateFormat("dd", Locale.US).format(dateObj)
            }
        } catch (e: Exception) {
            holder.month.text = "JAN"
            holder.day.text = "01"
        }
        
        // --- 2. Time Parsing (12h Format) ---
        val timeFormatted = try {
            val sessionTime = log.time ?: "00:00:00"
            val sdf24 = SimpleDateFormat("HH:mm:ss", Locale.US)
            val sdf12 = SimpleDateFormat("hh:mm a", Locale.US)
            val dateObj = sdf24.parse(sessionTime)
            sdf12.format(dateObj!!)
        } catch (e: Exception) {
            (log.time ?: "00:00").substringBeforeLast(":") // Fallback
        }
        // Show time + duration if available, clean right column
        val duration = log.duration
        holder.time.text = if (!duration.isNullOrBlank()) "$timeFormatted · $duration" else timeFormatted
        
        // --- 3. Coach & Service ---
        holder.service.text = log.service ?: "Service"
        val trainerName = log.trainer ?: ""
        holder.trainer.text = if (trainerName.isBlank() || trainerName.equals("Self", ignoreCase = true)) {
            "No Coach"
        } else {
            "Coach: $trainerName"
        }
        
        // --- 4. Status Styling ---
        val rawStatus = log.status?.uppercase() ?: "PENDING"
        val displayStatus = if (rawStatus == "CANCELLED") "REJECTED" else rawStatus
        holder.status.text = displayStatus

        val tintColor: String = when (rawStatus) {
            "APPROVED", "CONFIRMED", "ACTIVE" -> "#1A10B981"
            "PENDING" -> "#1AF59E0B"
            "REJECTED", "CANCELLED" -> "#1AEF4444"
            "COMPLETED" -> "#1AFFFFFF"
            else -> "#0DFFFFFF"
        }
        val textColor: String = when (rawStatus) {
            "APPROVED", "CONFIRMED", "ACTIVE" -> "#10B981"
            "PENDING" -> "#F59E0B"
            "REJECTED", "CANCELLED" -> "#EF4444"
            "COMPLETED" -> "#FFFFFF"
            else -> "#9CA3AF"
        }

        try {
            holder.status.setTextColor(Color.parseColor(textColor))
            holder.status.backgroundTintList = ColorStateList.valueOf(Color.parseColor(tintColor))
        } catch (e: Exception) {
            holder.status.setTextColor(Color.WHITE)
        }

        // --- 4.5. Cancel Button Visibility ---
        if (rawStatus in listOf("PENDING", "APPROVED", "CONFIRMED")) {
            holder.btnCancel.visibility = View.VISIBLE
            holder.btnCancel.setOnClickListener { onCancelClick(log) }
        } else {
            holder.btnCancel.visibility = View.GONE
            holder.btnCancel.setOnClickListener(null)
        }

        // --- 5. Branding Sync (mirrors TransactionAdapter) ---
        val themeColorStr = GymManager.getThemeColor(context)
        val themeColor = try { Color.parseColor(themeColorStr) } catch (e: Exception) { Color.parseColor("#8c2bee") }

        val cardColorStr = GymManager.getCardColor(context)
        val isAutoCard = GymManager.getAutoCardTheme(context) == "1"
        val cardBg = if (isAutoCard) {
            Color.argb(13, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
        } else {
            try { Color.parseColor(cardColorStr) } catch (e: Exception) { Color.parseColor("#141216") }
        }
        (holder.itemView as? com.google.android.material.card.MaterialCardView)
            ?.setCardBackgroundColor(ColorStateList.valueOf(cardBg))

        // Date block: theme-tinted background, theme-colored day, white month
        holder.cardDateBlock.setCardBackgroundColor(
            Color.argb(13, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
        )
        holder.day.setTextColor(themeColor)
        holder.month.setTextColor(Color.WHITE)
        holder.month.alpha = 1.0f
    }

    fun updateLogs(newLogs: List<TrainingLog>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    override fun getItemCount() = logs.size
}
