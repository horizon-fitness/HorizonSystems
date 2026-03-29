package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Notification
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NotificationSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.sheet_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvNotifications = view.findViewById<RecyclerView>(R.id.rvNotifications)
        val emptyState = view.findViewById<View>(R.id.tvEmptyState)

        val notifications = getMockNotifications()

        if (notifications.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvNotifications.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvNotifications.visibility = View.VISIBLE
            rvNotifications.adapter = NotificationAdapter(notifications)
        }
    }

    private fun getMockNotifications(): List<Notification> {
        return listOf(
            Notification(
                id = "1",
                title = "Welcome to Horizon!",
                message = "We're glad to have you. Enjoy your premium fitness experience.",
                time = "Just now",
                type = "system",
                isRead = false
            ),
            Notification(
                id = "2",
                title = "Membership Confirmed",
                message = "Your Unlimited Gym Plan is now active. Let's start training!",
                time = "2 hours ago",
                type = "membership",
                isRead = true
            ),
            Notification(
                id = "3",
                title = "Booking Success",
                message = "Your session for Personal Training with Coach Sam is confirmed for Monday at 10:00 AM.",
                time = "5 hours ago",
                type = "booking",
                isRead = true
            ),
            Notification(
                id = "4",
                title = "System Update",
                message = "The gym will be closed for maintenance on Sunday from 2:00 PM to 6:00 PM.",
                time = "Yesterday",
                type = "system",
                isRead = true
            )
        )
    }

    companion object {
        const val TAG = "NotificationSheet"
    }
}
