package com.taskraze.myapplication.view.friends

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.R
import com.taskraze.myapplication.view.chat.FriendlyMessageAdapter.Companion.TAG
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.taskraze.myapplication.model.calendar.UserData

class CustomFoundUsersAdapter(
    private val activity: AppCompatActivity,
    private val dataList: MutableList<UserData>,
    private val deleteButtonVisibility: Boolean
) : RecyclerView.Adapter<CustomFoundUsersAdapter.FoundUsersItemViewHolder>() {
    inner class FoundUsersItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePictureImageView: ImageView = itemView.findViewById(R.id.imageViewProfilePicture)
        val usernameTextView: TextView = itemView.findViewById(R.id.textViewUserName)
        val deleteImageButton: ImageButton = itemView.findViewById(R.id.imageButtonDelete)
        lateinit var viewModel: MainViewModel
    }


    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): FoundUsersItemViewHolder {
        val itemView = LayoutInflater.from(view.context).inflate(R.layout.found_users_item_view, view, false)
        return FoundUsersItemViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: FoundUsersItemViewHolder, position: Int) {
        viewHolder.viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
        val currentItem = dataList[position]
        viewHolder.usernameTextView.text = currentItem.username
        viewHolder.profilePictureImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)

        if (!deleteButtonVisibility) {
            viewHolder.deleteImageButton.visibility = GONE
        }

        viewHolder.itemView.setOnClickListener {
            showFriendRequestDialog(position)
        }
    }


    override fun getItemCount() = dataList.size

    private fun showFriendRequestDialog(position: Int) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Send friend request?")
            .setPositiveButton("Yes") { _, _ ->
                //send friend request
                sendFriendRequest(dataList[position].email)
            }
            .setNegativeButton("No") { _, _ ->
            }
            .show()
    }

    private fun sendFriendRequest(receiverUserId: String) {
        val senderUserId = FirebaseAuth.getInstance().currentUser?.email

        if (senderUserId != null) {
            val db = Firebase.firestore
            val friendRequestsCollection = db.collection("friend_requests")

            val friendRequestData = hashMapOf(
                "receiverId" to receiverUserId,
                "senderId" to senderUserId,
                "status" to "pending"
            )

            friendRequestsCollection
                .add(friendRequestData)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Friend request sent with ID: ${documentReference.id}")
                    Toast.makeText(activity, "Friend request sent", Toast.LENGTH_SHORT).show()

                    activity.onBackPressedDispatcher.onBackPressed()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error sending friend request", e)
                    Toast.makeText(activity, "Failed to send friend request", Toast.LENGTH_SHORT).show()
                }
        }

    }
}
