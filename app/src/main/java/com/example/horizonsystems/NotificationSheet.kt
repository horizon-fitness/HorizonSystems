package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Notification
import com.example.horizonsystems.utils.ThemeUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.utils.GymManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationSheet : BottomSheetDialogFragment() {

    private lateinit var adapter: NotificationAdapter
    private var notificationList = mutableListOf<Notification>()

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
        val btnClearAll = view.findViewById<View>(R.id.btnClearAllNotifications)

        adapter = NotificationAdapter(notificationList) { notif ->
            deleteNotification(notif.notification_id)
        }
        rvNotifications.adapter = adapter

        fetchNotifications(emptyState, rvNotifications)

        btnClearAll?.setOnClickListener {
            clearAllNotifications()
        }

        ThemeUtils.applyThemeToView(view)
    }

    private fun fetchNotifications(emptyState: View, rvNotifications: View) {
        val ctx = requireContext()
        val userId = GymManager.getUserId(ctx)
        val gymId = GymManager.getTenantId(ctx)
        val cookie = GymManager.getBypassCookie(ctx)
        val ua = GymManager.getBypassUA(ctx)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getNotifications(userId, gymId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        notificationList = response.body()?.notifications?.toMutableList() ?: mutableListOf()
                        adapter.updateData(notificationList)
                        
                        val isEmpty = notificationList.isEmpty()
                        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
                        rvNotifications.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    } else {
                        emptyState.visibility = View.VISIBLE
                        rvNotifications.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    emptyState.visibility = View.VISIBLE
                    rvNotifications.visibility = View.GONE
                }
            }
        }
    }

    private fun deleteNotification(notifId: Int) {
        val ctx = requireContext()
        val userId = GymManager.getUserId(ctx)
        val cookie = GymManager.getBypassCookie(ctx)
        val ua = GymManager.getBypassUA(ctx)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.clearNotification(userId, notifId, 0)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        notificationList.removeAll { it.notification_id == notifId }
                        adapter.updateData(notificationList)
                        if (notificationList.isEmpty()) {
                            view?.findViewById<View>(R.id.tvEmptyState)?.visibility = View.VISIBLE
                            view?.findViewById<View>(R.id.rvNotifications)?.visibility = View.GONE
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun clearAllNotifications() {
        val ctx = requireContext()
        val userId = GymManager.getUserId(ctx)
        val cookie = GymManager.getBypassCookie(ctx)
        val ua = GymManager.getBypassUA(ctx)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.clearNotification(userId, 0, 1)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        notificationList.clear()
                        adapter.updateData(notificationList)
                        view?.findViewById<View>(R.id.tvEmptyState)?.visibility = View.VISIBLE
                        view?.findViewById<View>(R.id.rvNotifications)?.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {}
        }
    }

    companion object {
        const val TAG = "NotificationSheet"
    }
}
