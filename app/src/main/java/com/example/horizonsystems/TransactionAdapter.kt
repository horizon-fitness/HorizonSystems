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
        val card: com.google.android.material.card.MaterialCardView = view as com.google.android.material.card.MaterialCardView
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

        // 2. Dynamic Card Sync
        val cardColorStr = GymManager.getCardColor(context)
        val isAutoCard = GymManager.getAutoCardTheme(context) == "1"
        val cardColor = if (isAutoCard) {
            val r = Color.red(themeColor)
            val g = Color.green(themeColor)
            val b = Color.blue(themeColor)
            Color.argb(13, r, g, b)
        } else {
            try { Color.parseColor(cardColorStr) } catch(e: Exception) { Color.parseColor("#141216") }
        }
        holder.card.setCardBackgroundColor(ColorStateList.valueOf(cardColor))

        // 3. Date Parsing (Modern Date Block)
        parseAndSetDate(txn.date, holder.tvTxnMonth, holder.tvTxnDay)
        holder.tvTxnDay.setTextColor(themeColor)
        holder.tvTxnMonth.setTextColor(Color.WHITE) // User requested white month
        holder.tvTxnMonth.alpha = 1.0f

        // 4. Content
        holder.service.text = txn.service ?: "Membership Plan"
        holder.dateTime.text = txn.time ?: "Subscription Payment"
        holder.amount.text = "₱${txn.amount ?: "0.00"}"
        holder.reference.text = "Ref: ${txn.reference ?: "N/A"}"
        holder.reference.setTextColor(Color.WHITE) // User requested white reference
        holder.reference.alpha = 1.0f
        
        holder.status.text = txn.status ?: "Pending"
        
        // 4. Status Badge Branding (Match Tenant Theme for positive states)
        val statusStr = txn.status ?: "Pending"
        if (statusStr.contains("Approved", ignoreCase = true) || 
            statusStr.contains("Completed", ignoreCase = true) || 
            statusStr.contains("Paid", ignoreCase = true)) {
            holder.status.setTextColor(themeColor)
            holder.status.backgroundTintList = ColorStateList.valueOf(themeColor).withAlpha(30)
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
