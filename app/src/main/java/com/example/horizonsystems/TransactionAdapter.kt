package com.example.horizonsystems

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Transaction

class TransactionAdapter(private val transactions: List<Transaction>) :
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
        val txn = transactions[position]
        holder.service.text = txn.service
        holder.dateTime.text = "${txn.date}, ${txn.time}"
        holder.amount.text = "₱${txn.amount}"
        holder.status.text = txn.status
    }

    override fun getItemCount() = transactions.size
}
