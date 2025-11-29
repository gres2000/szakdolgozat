package com.taskraze.myapplication.view.chat

import AuthViewModelFactory
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.view.main.MainActivity
import com.taskraze.myapplication.R
import com.taskraze.myapplication.databinding.StartChatActivityBinding
import com.taskraze.myapplication.common.CustomUsersAdapter
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.taskraze.myapplication.model.calendar.UserData
import com.taskraze.myapplication.model.chat.ChatData
import com.taskraze.myapplication.viewmodel.MainViewModelFactory
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.launch

class StartChatActivity: AppCompatActivity(), CustomUsersAdapter.ChatActionListener {
    private lateinit var binding: StartChatActivityBinding
    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var startNewChat: FloatingActionButton
    private lateinit var chooseFriendRecyclerView: RecyclerView
    private lateinit var authViewModel: AuthViewModel
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StartChatActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(this)
        )[AuthViewModel::class.java]
        val factory = MainViewModelFactory(authViewModel.getUserId(), authViewModel.loggedInUser.value!!)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

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
            chatsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val chatDataList = mutableListOf<ChatData>()
                    val currentUserEmail = viewModel.auth.currentUser!!.email!!

                    dataSnapshot.children.forEach { chatSnapshot ->
                        val chatData = chatSnapshot.getValue(ChatData::class.java) ?: return@forEach

                        val userEmails = chatData.users.mapNotNull {
                            it.userId.takeIf { id -> id.isNotEmpty() } ?: it.email.takeIf { email -> email.isNotEmpty() }
                        }

                        val inTitle = chatData.title.contains(currentUserEmail)

                        if (currentUserEmail in userEmails || inTitle) {
                            chatDataList.add(chatData)
                        }
                    }

                    val adapter = CustomChatsAdapter(this@StartChatActivity, chatDataList)
                    chatsRecyclerView.adapter = adapter
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
    private fun showChooseFriendDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.choose_friend_dialog)

        chooseFriendRecyclerView = dialog.findViewById(R.id.chooseFriendRecyclerView)
        chooseFriendRecyclerView.layoutManager = GridLayoutManager(this, 3)

        viewModel.getFriends { friendList ->
            val adapter = CustomUsersAdapter(this, friendList.toMutableList(), this, null,false)
            adapter.setItemClickedPrompt(getString(R.string.start_chat_with_selected_user))
            chooseFriendRecyclerView.adapter = adapter
            (chooseFriendRecyclerView.adapter as CustomUsersAdapter).notifyDataSetChanged()

            dialog.setCancelable(true)

            dialog.show()
        }
    }

    override fun onUserClickConfirmed(receiverUser: UserData) {
        viewModel.viewModelScope.launch {
            viewModel.viewModelScope.launch {
                val newChat = viewModel.startNewChat(receiverUser)

                if (newChat != null) {
                    val intent = Intent(this@StartChatActivity, ChatActivity::class.java)
                    intent.putExtra("chatId", newChat.id)
                    intent.putExtra("chatName", receiverUser.email)

                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                else {
                    val intent = Intent(this@StartChatActivity, ChatActivity::class.java)
                    val id = '-' + viewModel.generateIdFromEmails(receiverUser.email, authViewModel.loggedInUser.value!!.email)
                    intent.putExtra("chatId", id)
                    intent.putExtra("chatName", receiverUser.email)

                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }
    }

}