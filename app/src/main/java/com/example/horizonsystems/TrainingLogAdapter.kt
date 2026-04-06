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
        val log = logs[position]
        holder.date.text = log.date
        // Format Time to 12h: hh:mm AM/PM
        val timeFormatted = try {
            val sdf24 = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            val sdf12 = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
            val dateObj = sdf24.parse(log.time)
            sdf12.format(dateObj!!)
        } catch (e: Exception) {
            log.time.substringBeforeLast(":") // Fallback
        }
        holder.time.text = "$timeFormatted - ${log.duration}"
        holder.service.text = log.service
        
        // Smart Trainer Label: "Self" vs "Trainer: Name"
        holder.trainer.text = if (log.trainer.equals("Self", ignoreCase = true)) "Self" else "Trainer: ${log.trainer}"
        
        // Status Mapping: "CANCELLED" -> "REJECTED" for clearer donor/member communication
        val displayStatus = if (log.status.equals("CANCELLED", ignoreCase = true)) "REJECTED" else log.status.uppercase()
        holder.status.text = displayStatus

        // Status coloring
        val tintColor: String = when (log.status.uppercase()) {
            "APPROVED", "CONFIRMED", "ACTIVE" -> "#1A10B981"
            "PENDING" -> "#1AF59E0B"
            "REJECTED", "CANCELLED" -> "#1AEF4444"
            "COMPLETED" -> "#1AFFFFFF"
            else -> "#0DFFFFFF"
        }
        val textColor: String = when (log.status.uppercase()) {
            "APPROVED", "CONFIRMED", "ACTIVE" -> "#10B981"
            "PENDING" -> "#F59E0B"
            "REJECTED", "CANCELLED" -> "#EF4444"
            "COMPLETED" -> "#FFFFFF"
            else -> "#9CA3AF"
        }

        holder.status.setTextColor(android.graphics.Color.parseColor(textColor))
        holder.status.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(tintColor))
    }

    fun updateLogs(newLogs: List<TrainingLog>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    override fun getItemCount() = logs.size
}
