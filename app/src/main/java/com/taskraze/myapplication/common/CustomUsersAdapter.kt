package com.taskraze.myapplication.common

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.R
import com.taskraze.myapplication.model.calendar.UserData
import com.taskraze.myapplication.viewmodel.MainViewModel

class CustomUsersAdapter(private val activity: AppCompatActivity, private val dataList: MutableList<UserData>, private val chatActionListener: ChatActionListener?, private val deleteActionListener: DeleteActionListener?, private val deleteButtonVisibility: Boolean) : RecyclerView.Adapter<CustomUsersAdapter.FriendsItemViewHolder>() {
    private lateinit var dialogPrompt: String

    inner class FriendsItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePictureImageView: ImageView = itemView.findViewById(R.id.imageViewProfilePicture)
        val usernameTextView: TextView = itemView.findViewById(R.id.textViewUserName)
        val deleteImageButton: ImageButton = itemView.findViewById(R.id.imageButtonDelete)
        lateinit var viewModel: MainViewModel
    }

    interface ChatActionListener {
        fun onUserClickConfirmed(receiverUser: UserData)

    }

    interface DeleteActionListener {

        fun onDeleteConfirmed(deletedUser: UserData, position: Int)
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

        if (!deleteButtonVisibility) {
            viewHolder.deleteImageButton.visibility = GONE
        }

        viewHolder.deleteImageButton.setOnClickListener {
            showInitiateChatDialog(viewHolder.layoutPosition,
                activity.getString(R.string.are_you_sure_you_want_to_delete_this_user),
                DELETE_BUTTON_CLICKED
            )
        }

        if (chatActionListener != null) {
            viewHolder.itemView.setOnClickListener {
                showInitiateChatDialog(position, dialogPrompt, ITEM_CLICKED)
            }
        }
        else {
            viewHolder.itemView.findViewById<ConstraintLayout>(R.id.foundUsersConstraintLayout).isClickable = false
        }
    }


    override fun getItemCount() = dataList.size

    private fun showInitiateChatDialog(position: Int, dialogPrompt: String, option: Int) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(dialogPrompt)
            .setPositiveButton("Yes") { _, _ ->
                if (option == ITEM_CLICKED) {
                    chatActionListener!!.onUserClickConfirmed(dataList[position])
                }
                else if (option == DELETE_BUTTON_CLICKED){
                    deleteActionListener!!.onDeleteConfirmed(dataList[position], position)
                }
            }
            .setNegativeButton("No") { _, _ ->
            }
            .show()
    }


    fun setItemClickedPrompt(prompt: String) {
        dialogPrompt = prompt
    }

    fun addItem(receiverUser: UserData) {
        dataList.add(receiverUser)
    }

    fun removeItem(deletedUser: UserData) {
        dataList.remove(deletedUser)
    }

    companion object {
        const val ITEM_CLICKED = 0
        const val DELETE_BUTTON_CLICKED = 1
    }


}