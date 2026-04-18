package com.example.horizonsystems

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Transaction
import com.example.horizonsystems.utils.GymManager
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(private var transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTxnMonth: TextView = view.findViewById(R.id.tvTxnMonth)
        val tvTxnDay: TextView = view.findViewById(R.id.tvTxnDay)
        val service: TextView = view.findViewById(R.id.txnService)
        val dateTime: TextView = view.findViewById(R.id.txnDateTime)
        val amount: TextView = view.findViewById(R.id.txnAmount)
        val status: TextView = view.findViewById(R.id.txnStatus)
        val reference: TextView = view.findViewById(R.id.txnReference)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val txn = transactions.getOrNull(position) ?: return
        val context = holder.itemView.context
        
        // 1. Dynamic Branding Fetch
        val themeColorStr = GymManager.getThemeColor(context)
        val themeColor = try { Color.parseColor(themeColorStr) } catch (e: Exception) { Color.parseColor("#8c2bee") }

        // 2. Date Parsing (Modern Date Block)
        parseAndSetDate(txn.date, holder.tvTxnMonth, holder.tvTxnDay)
        holder.tvTxnDay.setTextColor(themeColor)

        // 3. Content
        holder.service.text = txn.service ?: "Membership Plan"
        holder.dateTime.text = txn.time ?: "Subscription Payment"
        holder.amount.text = "₱${txn.amount ?: "0.00"}"
        holder.reference.text = "Ref: ${txn.reference ?: "N/A"}"
        
        holder.status.text = txn.status ?: "Pending"
        
        // 4. Status Badge Branding
        val statusStr = txn.status ?: "Pending"
        if (statusStr.contains("Approved", ignoreCase = true) || statusStr.contains("Completed", ignoreCase = true)) {
            holder.status.setTextColor(Color.parseColor("#34D399")) // Emerald 400
            holder.status.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#34D399")).withAlpha(30)
        } else if (statusStr.contains("Rejected", ignoreCase = true) || statusStr.contains("Cancelled", ignoreCase = true)) {
            holder.status.setTextColor(Color.parseColor("#F87171")) // Red 400
            holder.status.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F87171")).withAlpha(30)
        } else {
            holder.status.setTextColor(Color.parseColor("#FBBF24")) // Amber 400
            holder.status.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FBBF24")).withAlpha(30)
        }
    }

    private fun parseAndSetDate(dateStr: String?, monthView: TextView, dayView: TextView) {
        if (dateStr.isNullOrEmpty()) {
            monthView.text = "---"
            dayView.text = "--"
            return
        }

        val formats = arrayOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("MMM d, yyyy", Locale.US),
            SimpleDateFormat("MMMM d, yyyy", Locale.US)
        )

        var date: Date? = null
        for (format in formats) {
            try {
                date = format.parse(dateStr)
                if (date != null) break
            } catch (e: Exception) { }
        }

        if (date != null) {
            monthView.text = SimpleDateFormat("MMM", Locale.US).format(date).uppercase()
            dayView.text = SimpleDateFormat("d", Locale.US).format(date)
        } else {
            monthView.text = "VAR"
            dayView.text = "??"
        }
    }

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    override fun getItemCount() = transactions.size
}
