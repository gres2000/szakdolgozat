package com.taskraze.myapplication.view.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.R
import com.taskraze.myapplication.model.friends.FriendRequestData
import com.taskraze.myapplication.viewmodel.MainViewModel

class CustomFriendRequestAdapter (private val onButtonClickListener: OnAcceptButtonClickedListener, private val activity: AppCompatActivity, private val dataList: MutableList<FriendRequestData>) : RecyclerView.Adapter<CustomFriendRequestAdapter.FriendRequestItemViewHolder>() {
    inner class FriendRequestItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePictureImageView: ImageView = itemView.findViewById(R.id.imageViewProfilePictureRequest)
        val usernameTextView: TextView = itemView.findViewById(R.id.textViewSenderNameRequest)
        lateinit var viewModel: MainViewModel
    }
    interface OnAcceptButtonClickedListener {
        fun onButtonClicked(position: Int)
    }
    override fun getItemViewType(position: Int): Int {
        return if (dataList[position].receiverId == MainViewModel.loggedInUser!!.email) {
            0
        } else {
            1
        }
    }

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): FriendRequestItemViewHolder {
        return if (viewType == 0) {
            val itemView = LayoutInflater.from(view.context).inflate(R.layout.friend_request_item_view, view, false)
            FriendRequestItemViewHolder(itemView)
        } else {
            val itemView = LayoutInflater.from(view.context).inflate(R.layout.friend_request_pending_item_view, view, false)
            FriendRequestItemViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(viewHolder: FriendRequestItemViewHolder, position: Int) {
        viewHolder.viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
        val currentItem = dataList[position]
        viewHolder.profilePictureImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)

        if (viewHolder.itemViewType == 0) {

            viewHolder.usernameTextView.text = currentItem.senderId
            val acceptImageButton: ImageButton = viewHolder.itemView.findViewById(R.id.imageButtonAcceptRequest)
            val rejectImageButton: ImageButton = viewHolder.itemView.findViewById(R.id.imageButtonRejectRequest)

            acceptImageButton.setOnClickListener {
                viewHolder.viewModel.handleFriendRequest("accepted", currentItem) { success ->
                    if (success) {
                        Toast.makeText(activity, "Friend request accepted.", Toast.LENGTH_SHORT).show()
                        dataList.removeAt(position)
                        notifyItemRemoved(position)
                        onButtonClickListener.onButtonClicked(position)
                    } else {
                        Toast.makeText(activity, "Couldn't accept request. Try again later.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            rejectImageButton.setOnClickListener {
                viewHolder.viewModel.handleFriendRequest("rejected", currentItem) { success ->
                    if (success) {
                        Toast.makeText(activity, "Friend request rejected.", Toast.LENGTH_SHORT).show()
                        dataList.removeAt(position)
                        notifyItemRemoved(position)
                    } else {
                        Toast.makeText(activity, "Couldn't reject request. Try again later.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        else {

            viewHolder.usernameTextView.text = currentItem.receiverId
        }



    }


    override fun getItemCount() = dataList.size
}
