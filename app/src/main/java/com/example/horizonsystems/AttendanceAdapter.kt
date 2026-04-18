package com.example.horizonsystems

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.utils.GymManager
import com.example.horizonsystems.utils.ThemeUtils
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

data class GymAttendance(
    val date: String,     // "yyyy-MM-dd"
    val time: String,     // "HH:mm" or "hh:mm a"
    val gymName: String,
    val status: String    // "PRESENT", "ABSENT"
)

class AttendanceAdapter(private var logs: List<GymAttendance>) : RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMonth: TextView = view.findViewById(R.id.tvAttendanceMonth)
        val tvDay: TextView = view.findViewById(R.id.tvAttendanceDay)
        val tvGymName: TextView = view.findViewById(R.id.tvGymName)
        val tvTime: TextView = view.findViewById(R.id.tvCheckInTime)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusBadge)
        val cardDateBlock: MaterialCardView = view.findViewById(R.id.cardDateBlock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]
        val context = holder.itemView.context
        val themeColor = Color.parseColor(GymManager.getThemeColor(context))

        // 1. Date Logic
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(log.date)
            holder.tvMonth.text = SimpleDateFormat("MMM", Locale.US).format(date!!).uppercase()
            holder.tvDay.text = SimpleDateFormat("dd", Locale.US).format(date)
        } catch (e: Exception) {
            holder.tvMonth.text = "UNK"
            holder.tvDay.text = "00"
        }

        // 2. Info Logic
        holder.tvGymName.text = log.gymName
        holder.tvTime.text = log.time
        holder.tvStatus.text = log.status

        // 3. Branding
        holder.tvMonth.setTextColor(themeColor)
        holder.tvStatus.setTextColor(themeColor)
        holder.cardDateBlock.setCardBackgroundColor(ColorStateList.valueOf(themeColor).withAlpha(20))
        
        // 4. Root Card Theme (Parity with Transaction)
        val cardView = holder.itemView as? MaterialCardView
        cardView?.let {
            val isAutoCard = GymManager.getAutoCardTheme(context) == "1"
            if (isAutoCard) {
                it.setCardBackgroundColor(ColorStateList.valueOf(themeColor).withAlpha(13))
                it.setStrokeColor(ColorStateList.valueOf(themeColor).withAlpha(40))
            } else {
                val cardColor = try { Color.parseColor(GymManager.getCardColor(context)) } catch(e: Exception) { Color.parseColor("#0D0D10") }
                it.setCardBackgroundColor(cardColor)
                it.setStrokeColor(ColorStateList.valueOf(Color.WHITE).withAlpha(26))
            }
        }
    }

    override fun getItemCount() = logs.size

    fun updateLogs(newLogs: List<GymAttendance>) {
        this.logs = newLogs
        notifyDataSetChanged()
    }
}
