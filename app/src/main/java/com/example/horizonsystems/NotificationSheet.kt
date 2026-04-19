package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.Notification
import com.example.horizonsystems.utils.ThemeUtils
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.horizonsystems.network.RetrofitClient
import android.widget.ProgressBar

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

        ThemeUtils.applyThemeToView(view)
        applyBranding(view)
        
        fetchNotifications(view)
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = com.example.horizonsystems.utils.GymManager.getThemeColor(ctx)
        val bgColorStr = com.example.horizonsystems.utils.GymManager.getBgColor(ctx)
        
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                view.findViewById<TextView>(R.id.sheetSubtitle)?.setTextColor(themeColor)
                
                // Theme the progress bar too
                view.findViewById<ProgressBar>(R.id.pbNotifications)?.indeterminateTintList = 
                    android.content.res.ColorStateList.valueOf(themeColor)
            } catch (e: Exception) {}
        }

        if (!bgColorStr.isNullOrEmpty()) {
            try {
                val bgColor = android.graphics.Color.parseColor(bgColorStr)
                val shape = android.graphics.drawable.GradientDrawable()
                shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                shape.setColor(bgColor)
                val radius = (28 * ctx.resources.displayMetrics.density)
                shape.cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                view.background = shape
            } catch (e: Exception) {}
        }
    }

    private fun fetchNotifications(view: View) {
        val rvNotifications = view.findViewById<RecyclerView>(R.id.rvNotifications)
        val emptyState = view.findViewById<View>(R.id.tvEmptyState)
        val progressBar = view.findViewById<ProgressBar>(R.id.pbNotifications)
        
        val ctx = context ?: return
        val userId = GymManager.getUserId(ctx)
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(GymManager.getBypassCookie(ctx), GymManager.getBypassUA(ctx))
                val response = api.getNotifications(userId)
                
                withContext(Dispatchers.Main) {
                    progressBar?.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val notifications = response.body()?.notifications ?: emptyList()
                        if (notifications.isEmpty()) {
                            emptyState.visibility = View.VISIBLE
                            rvNotifications.visibility = View.GONE
                        } else {
                            emptyState.visibility = View.GONE
                            rvNotifications.visibility = View.VISIBLE
                            rvNotifications.adapter = NotificationAdapter(notifications)
                        }
                    } else {
                        // Fallback to mock on failure
                        loadMockNotifications(rvNotifications, emptyState)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar?.visibility = View.GONE
                    loadMockNotifications(rvNotifications, emptyState)
                }
            }
        }
    }

    private fun loadMockNotifications(rv: RecyclerView, empty: View) {
        val mocks = getMockNotifications()
        empty.visibility = if (mocks.isEmpty()) View.VISIBLE else View.GONE
        rv.visibility = if (mocks.isEmpty()) View.GONE else View.VISIBLE
        rv.adapter = NotificationAdapter(mocks)
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
            )
        )
    }

    companion object {
        const val TAG = "NotificationSheet"
    }
}
