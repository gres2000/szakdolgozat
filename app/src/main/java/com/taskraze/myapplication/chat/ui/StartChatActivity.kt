package com.taskraze.myapplication.chat.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.main.MainActivity
import com.taskraze.myapplication.R
import com.taskraze.myapplication.room_database.data_classes.User
import com.taskraze.myapplication.databinding.StartChatActivityBinding
import com.taskraze.myapplication.main.common.CustomUsersAdapter
import com.taskraze.myapplication.view_model.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.taskraze.myapplication.chat.data_classes.ChatData
import kotlinx.coroutines.launch

class StartChatActivity: AppCompatActivity(), CustomUsersAdapter.ChatActionListener {
    private lateinit var binding: StartChatActivityBinding
    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var startNewChat: FloatingActionButton
    private lateinit var chooseFriendRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StartChatActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@StartChatActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        })

        chatsRecyclerView = findViewById(R.id.recyclerViewChats)
        startNewChat = findViewById(R.id.fab_start_new_chat)
        chatsRecyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val database = FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
            val chatsRef = database.getReference("chats")
            val chatDataList = mutableListOf<ChatData>()
            chatsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    dataSnapshot.children.forEach { chatSnapshot ->
                        val chatData = chatSnapshot.getValue(ChatData::class.java)
                        if (chatData != null && MainViewModel.auth.currentUser!!.email!! in chatData.users.map { it.email }) {
                            chatDataList.add(chatData)
                        }
                        val adapter = CustomChatsAdapter(this@StartChatActivity, chatDataList.toMutableList())
                        chatsRecyclerView.adapter = adapter
                        chatsRecyclerView.adapter?.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@StartChatActivity, "An error occured with fatabase", Toast.LENGTH_SHORT).show()
                }
            })





        }

        startNewChat.setOnClickListener{
            MainViewModel.viewModelScope.launch {
                showChooseFriendDialog()

            }
        }
    }
    private suspend fun showChooseFriendDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.choose_friend_dialog)

        // Find views in the dialog layout
        chooseFriendRecyclerView = dialog.findViewById(R.id.chooseFriendRecyclerView)
        chooseFriendRecyclerView.layoutManager = GridLayoutManager(this, 3)

        MainViewModel.getFriends { friendList ->
            val adapter = CustomUsersAdapter(this, friendList.toMutableList(), this, null,false)
            adapter.setItemClickedPrompt(getString(R.string.start_chat_with_selected_user))
            chooseFriendRecyclerView.adapter = adapter
            (chooseFriendRecyclerView.adapter as CustomUsersAdapter).notifyDataSetChanged()

            dialog.setCancelable(true)

            dialog.show()
        }
    }

    override fun onUserClickConfirmed(receiverUser: User) {
        MainViewModel.viewModelScope.launch {
            MainViewModel.viewModelScope.launch {
                val newChat = MainViewModel.startNewChat(receiverUser)

                if (newChat != null) {
                    val intent = Intent(this@StartChatActivity, ChatActivity::class.java)
                    intent.putExtra("chatId", newChat.id)
                    intent.putExtra("chatName", receiverUser.email)

                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                else {
                    val intent = Intent(this@StartChatActivity, ChatActivity::class.java)
                    val id = '-' + MainViewModel.generateIdFromEmails(receiverUser.email, MainViewModel.loggedInUser!!.email)
                    intent.putExtra("chatId", id)
                    intent.putExtra("chatName", receiverUser.email)

                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }
    }

}