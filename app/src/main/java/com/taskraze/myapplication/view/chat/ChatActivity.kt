package com.taskraze.myapplication.view.chat

import AuthViewModelFactory
import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskraze.myapplication.databinding.ChatActivityBinding
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DatabaseReference
import com.taskraze.myapplication.model.chat.FriendlyMessage
import com.taskraze.myapplication.viewmodel.MainViewModelFactory
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel

class ChatActivity: AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var binding: ChatActivityBinding
    private lateinit var manager: LinearLayoutManager

    private lateinit var auth: FirebaseAuth
    private var db = FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
    private lateinit var chatRef: DatabaseReference
    private lateinit var adapter: FriendlyMessageAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ChatActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(this)
        )[AuthViewModel::class.java]

        val factory = MainViewModelFactory(authViewModel.getUserId(), authViewModel.loggedInUser.value!!)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        auth = viewModel.auth

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@ChatActivity, StartChatActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        })

        val allChatsRef = db.getReference("chats")
        chatRef = allChatsRef.child(intent.getStringExtra("chatId")!!)
        val messagesRef = chatRef.child("messages")

        val options = FirebaseRecyclerOptions.Builder<FriendlyMessage>()
            .setQuery(messagesRef, FriendlyMessage::class.java)
            .build()
        adapter = FriendlyMessageAdapter(options, getUserName(), authViewModel)
        binding.progressBar.visibility = ProgressBar.INVISIBLE
        manager = LinearLayoutManager(this)
        manager.stackFromEnd = true
        binding.messageRecyclerView.layoutManager = manager
        binding.messageRecyclerView.adapter = adapter
        binding.chatTitle.text = intent.getStringExtra("chatName")

        // scroll to bottom
        adapter.registerAdapterDataObserver(
            MyScrollToBottomObserver(binding.messageRecyclerView, adapter, manager)
        )

        binding.messageEditText.addTextChangedListener(MyButtonObserver(binding.sendButton))

        binding.imageButtonReturn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.sendButton.setOnClickListener {
            val friendlyMessage = FriendlyMessage(
                binding.messageEditText.text.toString(),
                getUserName(),
                getPhotoUrl(),
            )
            chatRef.child("messages").push().setValue(friendlyMessage)
            binding.messageEditText.setText("")
        }
    }

    public override fun onPause() {
        adapter.stopListening()
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        adapter.startListening()
    }

    private fun getPhotoUrl(): String? {
        val user = auth.currentUser
        return user?.photoUrl?.toString()
    }

    private fun getUserName(): String {
        return if (authViewModel.loggedInUser.value != null) {
            authViewModel.loggedInUser.value!!.username
        } else ANONYMOUS
    }

    companion object {
        const val ANONYMOUS = "anonymous"
    }

}