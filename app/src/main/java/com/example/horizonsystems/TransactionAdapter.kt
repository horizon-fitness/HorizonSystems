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
        val date: TextView = view.findViewById(R.id.txnDate)
        val time: TextView = view.findViewById(R.id.txnTime)
        val service: TextView = view.findViewById(R.id.txnService)
        val reference: TextView = view.findViewById(R.id.txnRef)
        val amount: TextView = view.findViewById(R.id.txnAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val txn = transactions[position]
        holder.date.text = txn.date
        holder.time.text = txn.time
        holder.service.text = txn.service
        holder.reference.text = "Ref: ${txn.reference}"
        holder.amount.text = "₱${txn.amount}"
    }

    override fun getItemCount() = transactions.size
}
