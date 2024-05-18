package com.example.myapplication.chat

import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.viewModel.MainViewModel
import kotlinx.coroutines.launch

class CustomChatsAdapter(private val activity: AppCompatActivity, private val dataList: MutableList<ChatData>) : RecyclerView.Adapter<CustomChatsAdapter.ChatItemViewHolder>() {
    inner class ChatItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitleCalendarItem)
        val numberTextView: TextView = itemView.findViewById(R.id.textViewPeopleNumber)
        val deleteImageButton: ImageButton = itemView.findViewById(R.id.imageButtonDelete)
        val placeholderTextView: TextView = itemView.findViewById(R.id.textViewLastUpdated)
        var id = ""
        var viewHolderId: Int = -1
        lateinit var viewModel: MainViewModel
    }

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): ChatItemViewHolder {
        val itemView = LayoutInflater.from(view.context).inflate(R.layout.calendar_item_view, view, false)
        return ChatItemViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: ChatItemViewHolder, position: Int) {
        viewHolder.viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
        val currentItem = dataList[position]
        if (currentItem.id.first() == '-') {
            val splitTitle = currentItem.title.split('&')
            if (splitTitle[0] == viewHolder.viewModel.auth.currentUser!!.email!!) {
                viewHolder.titleTextView.text = splitTitle[1]
            }
            else {
                viewHolder.titleTextView.text = splitTitle[0]
            }
        }
        val tempString = "People: " + currentItem.users.size
        viewHolder.numberTextView.text = tempString
        viewHolder.id = currentItem.id
        viewHolder.placeholderTextView.text = ""

        viewHolder.deleteImageButton.setOnClickListener{
            showQuitDialog(viewHolder.layoutPosition)
        }

        viewHolder.itemView.setOnClickListener{
            val intent = Intent(activity, ChatActivity::class.java)
            intent.putExtra("chatId", viewHolder.id)
            intent.putExtra("chatName", viewHolder.titleTextView.text)
            activity.startActivity(intent)
        }
    }
    override fun getItemCount() = dataList.size

    fun updateData(newData: List<ChatData>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyItemInserted(dataList.size)
    }

    private fun showQuitDialog(position: Int) {
        val builder = AlertDialog.Builder(activity)
        val inflater = LayoutInflater.from(activity)
        val dialogView = inflater.inflate(R.layout.delete_dialog, null)
        builder.setView(dialogView)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message)
        messageTextView.text = activity.getString(R.string.quit_chat)

        val buttonCancel = dialogView.findViewById<Button>(R.id.button_cancel)
        val buttonDelete = dialogView.findViewById<Button>(R.id.button_delete)

        val dialog = builder.create()

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        buttonDelete.setOnClickListener {
            // Perform the delete action here
            // For example, call a method to delete the item from your data source
            val viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
            viewModel.viewModelScope.launch { // Launch a coroutine
                MainViewModel.quitChat(activity, dataList[position])
                dataList.removeAt(position)
                notifyItemRemoved(position)
            }
            dialog.dismiss()
        }

        dialog.show()
    }
}