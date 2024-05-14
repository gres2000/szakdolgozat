package com.example.myapplication.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.authentication.User
import com.example.myapplication.viewModel.MainViewModel

class CustomFriendsAdapter(private val activity: AppCompatActivity, private val dataList: MutableList<User>) : RecyclerView.Adapter<CustomFriendsAdapter.FriendsItemViewHolder>() {
    inner class FriendsItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePictureImageView: ImageView = itemView.findViewById(R.id.imageViewProfilePicture)
        val usernameTextView: TextView = itemView.findViewById(R.id.textViewUserName)
        lateinit var viewModel: MainViewModel
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

        viewHolder.itemView.setOnClickListener {
            showInitiateChatDialog(position)
        }
    }


    override fun getItemCount() = dataList.size

    private fun showInitiateChatDialog(position: Int) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Start a chat with selected user?")
            .setPositiveButton("Yes") { _, _ ->
                //send friend request
                initiateChat(dataList[position].email)
            }
            .setNegativeButton("No") { _, _ ->
            }
            .show()
    }

    private fun initiateChat(receiverUserId: String) {
        //start a chat

    }
}