package com.example.horizonsystems

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Appointment

class AppointmentAdapter(private val appointments: List<Appointment>) :
    RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val subject: TextView = view.findViewById(R.id.txnService) // Reuse item_transaction layout
        val date: TextView = view.findViewById(R.id.txnDate)
        val time: TextView = view.findViewById(R.id.txnTime)
        val status: TextView = view.findViewById(R.id.txnAmount) // Repurpose for status text
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Reusing item_transaction.xml for consistency
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appt = appointments[position]
        holder.subject.text = appt.subject
        holder.date.text = appt.date
        holder.time.text = appt.time
        holder.status.text = appt.status.uppercase()

        val context = holder.itemView.context
        when (appt.status.lowercase()) {
            "approved" -> {
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.emerald_400))
            }
            "pending" -> {
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.amber_500))
            }
            else -> {
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
    }

    override fun getItemCount() = appointments.size
}
