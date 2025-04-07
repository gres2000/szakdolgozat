package com.taskraze.myapplication.view.friends

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.widget.SearchView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.R
import com.taskraze.myapplication.model.room_database.data_classes.User
import com.taskraze.myapplication.view.chat.ChatActivity
import com.taskraze.myapplication.common.CustomUsersAdapter
import com.taskraze.myapplication.databinding.FriendsActivityBinding
import com.taskraze.myapplication.model.auth.AuthRepository
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.launch

class FriendsActivity : AppCompatActivity(), CustomFriendRequestAdapter.OnAcceptButtonClickedListener, CustomUsersAdapter.ChatActionListener, CustomUsersAdapter.DeleteActionListener {

    private val authRepository = AuthRepository()
    private lateinit var binding: FriendsActivityBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var searchBarSearchView: SearchView
    private lateinit var searchButtonImageButton: ImageButton
    private lateinit var friendRequestsRecyclerView: RecyclerView
    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var friendRequestNumberTextView: TextView
    private val searchViewQueryListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            // Called when the user submits the query (e.g., presses Enter)
            if (!query.isNullOrEmpty()) {
                val intent = Intent(this@FriendsActivity, FoundUsersActivity::class.java)
                intent.putExtra("searchQuery", query)
                startActivity(intent)
            }
            return true // Return true to indicate that the query has been handled
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            // Called when the query text changes (e.g., user types)
            // You can perform search action dynamically as the user types, if desired
            return true // Return true to indicate that the query change has been handled
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FriendsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        friendRequestsRecyclerView = findViewById(R.id.friendRequestsRecyclerView)
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView)
        friendRequestNumberTextView = findViewById(R.id.friendRequestNumberTextView)

        searchBarSearchView = findViewById(R.id.searchViewFriends)
        searchButtonImageButton = findViewById(R.id.searchButton)

        lifecycleScope.launch {
            // authRepository.fetchUserDetails()
        }
        // Initialize your views or perform any other setup here
        searchBarSearchView.setOnQueryTextListener(searchViewQueryListener)

        friendRequestsRecyclerView.layoutManager = LinearLayoutManager(this)
        friendsRecyclerView.layoutManager = GridLayoutManager(this, 3)

        lifecycleScope.launch {
            // authRepository.fetchUserDetails()
            MainViewModel.getFriendRequests { friendRequests ->
                val adapter = CustomFriendRequestAdapter(this@FriendsActivity, this@FriendsActivity, friendRequests.toMutableList())
                friendRequestsRecyclerView.adapter = adapter
                (friendRequestsRecyclerView.adapter as CustomFriendRequestAdapter).notifyDataSetChanged()
                val tempString = getString(R.string.friend_requests) + " " +adapter.itemCount
                friendRequestNumberTextView.text = tempString
            }
        }

        lifecycleScope.launch {
            // authRepository.fetchUserDetails()
            MainViewModel.getFriends { friends ->
                val adapter = CustomUsersAdapter(this@FriendsActivity, friends.toMutableList(), this@FriendsActivity, this@FriendsActivity, true)
                adapter.setItemClickedPrompt(getString(R.string.start_chat_with_selected_user))
                friendsRecyclerView.adapter = adapter
                (friendsRecyclerView.adapter as CustomUsersAdapter).notifyDataSetChanged()
            }
        }


        searchButtonImageButton.setOnClickListener {
            val query = searchBarSearchView.query.toString()
            searchBarSearchView.onActionViewExpanded()
            searchBarSearchView.clearFocus()
            searchBarSearchView.setQuery(query, true)
        }




    }

    override fun onButtonClicked(position: Int) {
        lifecycleScope.launch {
            // authRepository.fetchUserDetails()
            viewModel.getFriends { friends ->
                val adapter = CustomUsersAdapter(this@FriendsActivity, friends.toMutableList(), this@FriendsActivity, null, true)
                adapter.setItemClickedPrompt(getString(R.string.start_chat_with_selected_user))
                adapter.setItemClickedPrompt(getString(R.string.start_chat_with_selected_user))
                friendsRecyclerView.adapter = adapter
                (friendsRecyclerView.adapter as CustomUsersAdapter).notifyDataSetChanged()

                val tempString = getString(R.string.friend_requests) + " " + (friendRequestNumberTextView.text.last().digitToInt() - 1)
                friendRequestNumberTextView.text = tempString
            }
        }
    }

    override fun onUserClickConfirmed(receiverUser: User) {
        MainViewModel.viewModelScope.launch {
            val newChat = MainViewModel.startNewChat(receiverUser)

            if (newChat != null) {
                val intent = Intent(this@FriendsActivity, ChatActivity::class.java)
                intent.putExtra("chatId", newChat.id)
                intent.putExtra("chatName", receiverUser.email)

                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            else {
                val intent = Intent(this@FriendsActivity, ChatActivity::class.java)
                val id = '-' + MainViewModel.generateIdFromEmails(receiverUser.email, AuthViewModel.loggedInUser!!.email)
                intent.putExtra("chatId", id)
                intent.putExtra("chatName", receiverUser.email)

                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    override fun onDeleteConfirmed(deletedUser: User, position: Int) {
        MainViewModel.viewModelScope.launch {
            MainViewModel.removeUserFromFriends(this@FriendsActivity, deletedUser)
            (binding.friendsRecyclerView.adapter as CustomUsersAdapter).removeItem(deletedUser)
            binding.friendsRecyclerView.adapter!!.notifyItemRemoved(position)
        }
    }
}