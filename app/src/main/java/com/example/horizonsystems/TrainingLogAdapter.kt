package com.example.horizonsystems

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.TrainingLog

class TrainingLogAdapter(private var logs: List<TrainingLog>) :
    RecyclerView.Adapter<TrainingLogAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.logDate)
        val time: TextView = view.findViewById(R.id.logTime)
        val service: TextView = view.findViewById(R.id.logService)
        val trainer: TextView = view.findViewById(R.id.logTrainer)
        val status: TextView = view.findViewById(R.id.logStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs.getOrNull(position) ?: return
        
        holder.date.text = log.date ?: "N/A"
        
        // Format Time to 12h: hh:mm AM/PM (Null-Safe)
        val timeFormatted = try {
            val sessionTime = log.time ?: "00:00:00"
            val sdf24 = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            val sdf12 = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
            val dateObj = sdf24.parse(sessionTime)
            sdf12.format(dateObj!!)
        } catch (e: Exception) {
            (log.time ?: "00:00").substringBeforeLast(":") // Fallback
        }
        holder.time.text = "$timeFormatted - ${log.duration ?: "0m"}"
        holder.service.text = log.service ?: "Service"
        
        // Smart Trainer Label: "Self" vs "Trainer: Name"
        val trainerName = log.trainer ?: "Self"
        holder.trainer.text = if (trainerName.equals("Self", ignoreCase = true)) "Self" else "Trainer: $trainerName"
        
        // Status Mapping: "CANCELLED" -> "REJECTED" for clearer communication
        val rawStatus = log.status?.uppercase() ?: "PENDING"
        val displayStatus = if (rawStatus == "CANCELLED") "REJECTED" else rawStatus
        holder.status.text = displayStatus

        // Status coloring (Robust)
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
            holder.status.setTextColor(android.graphics.Color.parseColor(textColor))
            holder.status.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(tintColor))
        } catch (e: Exception) {
            holder.status.setTextColor(android.graphics.Color.WHITE)
        }
    }

    fun updateLogs(newLogs: List<TrainingLog>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    override fun getItemCount() = logs.size
}
