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
        val subject: TextView = view.findViewById(R.id.txnService)
        val dateTime: TextView = view.findViewById(R.id.txnDateTime)
        val status: TextView = view.findViewById(R.id.txnStatus)
        val amount: TextView = view.findViewById(R.id.txnAmount)
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
        holder.dateTime.text = "${appt.date}, ${appt.time}"
        holder.status.text = appt.status.uppercase()
        
        // Appointments don't use amount field, hide it to keep look clean
        holder.amount.visibility = View.GONE

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
