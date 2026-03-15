package com.example.horizonsystems

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.models.User

class UserAdapter(private var users: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.userName)
        val handleText: TextView = view.findViewById(R.id.userHandle)
        val emailText: TextView = view.findViewById(R.id.userEmail)
        val initialText: TextView = view.findViewById(R.id.userInitial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.nameText.text = "${user.firstName} ${user.lastName}"
        holder.handleText.text = "@${user.username}"
        holder.emailText.text = user.email
        holder.initialText.text = user.firstName.take(1).uppercase()
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
