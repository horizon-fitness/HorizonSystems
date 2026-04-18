package com.example.horizonsystems

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Transaction

class TransactionAdapter(private var transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val service: TextView = view.findViewById(R.id.txnService)
        val dateTime: TextView = view.findViewById(R.id.txnDateTime)
        val amount: TextView = view.findViewById(R.id.txnAmount)
        val status: TextView = view.findViewById(R.id.txnStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val txn = transactions.getOrNull(position) ?: return
        
        holder.service.text = txn.service ?: "Service"
        holder.dateTime.text = txn.date ?: "N/A"
        holder.amount.text = "₱${txn.amount ?: "0.00"}"
        holder.status.text = txn.status?.uppercase() ?: "PENDING"
        
        // Bind Reference
        val referenceText = holder.itemView.findViewById<TextView>(R.id.txnReference)
        referenceText?.text = "Ref: ${txn.reference ?: "N/A"}"
        referenceText?.visibility = if (txn.reference.isNullOrEmpty()) View.GONE else View.VISIBLE
        
        val context = holder.itemView.context ?: return
        try {
            val statusStr = txn.status ?: "Pending"
            if (statusStr.equals("Completed", ignoreCase = true) || statusStr.equals("Approved", ignoreCase = true)) {
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.emerald_400))
            } else {
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        } catch (e: Exception) {
            holder.status.setTextColor(android.graphics.Color.WHITE)
        }
    }

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    override fun getItemCount() = transactions.size
}
