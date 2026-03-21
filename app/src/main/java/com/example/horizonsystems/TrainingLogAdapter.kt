package com.example.horizonsystems

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.TrainingLog

class TrainingLogAdapter(private val logs: List<TrainingLog>) :
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
        holder.time.text = "${log.time} - ${log.duration}"
        holder.service.text = log.service
        holder.trainer.text = "Trainer: ${log.trainer}"
        holder.status.text = log.status

        // Status coloring
        val context = holder.itemView.context
        when (log.status.uppercase()) {
            "ACTIVE" -> {
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.emerald_400))
                holder.status.setBackgroundColor(android.graphics.Color.parseColor("#1A10B981"))
            }
            "COMPLETED" -> {
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.status.setBackgroundColor(android.graphics.Color.parseColor("#1AFFFFFF"))
            }
            else -> {
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                holder.status.setBackgroundColor(android.graphics.Color.parseColor("#0DFFFFFF"))
            }
        }
    }

    override fun getItemCount() = logs.size
}
