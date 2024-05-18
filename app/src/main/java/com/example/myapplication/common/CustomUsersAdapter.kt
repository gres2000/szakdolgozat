package com.example.myapplication.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.authentication.User
import com.example.myapplication.viewModel.MainViewModel

class CustomUsersAdapter(private val activity: AppCompatActivity, private val dataList: MutableList<User>, private val listener: ChatActionListener?) : RecyclerView.Adapter<CustomUsersAdapter.FriendsItemViewHolder>() {
    private lateinit var dialogPrompt: String

    inner class FriendsItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePictureImageView: ImageView = itemView.findViewById(R.id.imageViewProfilePicture)
        val usernameTextView: TextView = itemView.findViewById(R.id.textViewUserName)
        lateinit var viewModel: MainViewModel
    }

    interface ChatActionListener {
        fun onInitiateChat(receiverUser: User)
    }


    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): FriendsItemViewHolder {
        val itemView = LayoutInflater.from(view.context).inflate(R.layout.found_users_item_view, view, false)
        return FriendsItemViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: FriendsItemViewHolder, position: Int) {
        viewHolder.viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
        val currentItem = dataList[position]
        viewHolder.usernameTextView.text = currentItem.email
        viewHolder.profilePictureImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)

        if (listener != null) {
            viewHolder.itemView.setOnClickListener {
                showInitiateChatDialog(position)
            }
        }
        else {
            viewHolder.itemView.findViewById<LinearLayout>(R.id.foundUsersLinearLayout).isClickable = false
        }
    }


    override fun getItemCount() = dataList.size

    private fun showInitiateChatDialog(position: Int) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(dialogPrompt)
            .setPositiveButton("Yes") { _, _ ->
                listener!!.onInitiateChat(dataList[position])
            }
            .setNegativeButton("No") { _, _ ->
            }
            .show()
    }

    private fun initiateChat(receiverUserId: String) {
        //start a chat

    }

    fun setItemClickedPrompt(prompt: String) {
        dialogPrompt = prompt
    }
}