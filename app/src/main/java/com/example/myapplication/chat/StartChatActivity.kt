package com.example.myapplication.chat

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.authentication.User
import com.example.myapplication.databinding.StartChatActivityBinding
import com.example.myapplication.common.CustomUsersAdapter
import com.example.myapplication.viewModel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch

class StartChatActivity: AppCompatActivity(), CustomUsersAdapter.ChatActionListener {
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: StartChatActivityBinding
    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var startNewChat: FloatingActionButton
    private lateinit var chooseFriendRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StartChatActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

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
                        if (chatData != null && viewModel.auth.currentUser!!.email!! in chatData.users.map { it.email }) {
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
            viewModel.viewModelScope.launch {
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

        viewModel.getFriends { friendList ->
            val adapter = CustomUsersAdapter(this, friendList.toMutableList(), this)
            adapter.setItemClickedPrompt(getString(R.string.start_chat_with_selected_user))
            chooseFriendRecyclerView.adapter = adapter
            (chooseFriendRecyclerView.adapter as CustomUsersAdapter).notifyDataSetChanged()

            dialog.setCancelable(true)

            dialog.show()
        }
    }

    override fun onInitiateChat(receiverUser: User) {
        viewModel.viewModelScope.launch {

            viewModel.startNewChat(receiverUser)
        }
    }
}