package com.taskraze.myapplication.view.friends

import AuthViewModelFactory
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.taskraze.myapplication.view.chat.ChatActivity
import com.taskraze.myapplication.common.CustomUsersAdapter
import com.taskraze.myapplication.databinding.FriendsActivityBinding
import com.taskraze.myapplication.model.auth.AuthRepository
import com.taskraze.myapplication.model.calendar.UserData
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.taskraze.myapplication.viewmodel.MainViewModelFactory
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.launch

class FriendsActivity : AppCompatActivity(), CustomFriendRequestAdapter.OnAcceptButtonClickedListener, CustomUsersAdapter.ChatActionListener, CustomUsersAdapter.DeleteActionListener {

    private lateinit var binding: FriendsActivityBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var searchBarSearchView: SearchView
    private lateinit var searchButtonImageButton: ImageButton
    private lateinit var friendRequestsRecyclerView: RecyclerView
    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var friendRequestNumberTextView: TextView
    private val searchViewQueryListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            // disable lsitener
            searchBarSearchView.setOnQueryTextListener(null)

            if (!query.isNullOrEmpty()) {
                val intent = Intent(this@FriendsActivity, FoundUsersActivity::class.java)
                intent.putExtra("searchQuery", query)
                startActivity(intent)

                // re-enable listener
                searchBarSearchView.postDelayed({
                    searchBarSearchView.setOnQueryTextListener(this)
                }, 1000)
            }
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FriendsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(this)
        )[AuthViewModel::class.java]
        val factory = MainViewModelFactory(authViewModel.getUserId(), authViewModel.loggedInUser.value!!)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        friendRequestsRecyclerView = findViewById(R.id.friendRequestsRecyclerView)
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView)
        friendRequestNumberTextView = findViewById(R.id.friendRequestNumberTextView)

        searchBarSearchView = findViewById(R.id.searchViewFriends)
        searchButtonImageButton = findViewById(R.id.searchButton)

        lifecycleScope.launch {
        }
        searchBarSearchView.setOnQueryTextListener(searchViewQueryListener)

        friendRequestsRecyclerView.layoutManager = LinearLayoutManager(this)
        friendsRecyclerView.layoutManager = GridLayoutManager(this, 3)

        lifecycleScope.launch {
            viewModel.getFriendRequests { friendRequests ->
                val adapter = CustomFriendRequestAdapter(this@FriendsActivity, this@FriendsActivity, friendRequests.toMutableList())
                friendRequestsRecyclerView.adapter = adapter
                (friendRequestsRecyclerView.adapter as CustomFriendRequestAdapter).notifyDataSetChanged()
                val tempString = getString(R.string.friend_requests) + " " +adapter.itemCount
                friendRequestNumberTextView.text = tempString
            }

            viewModel.getFriends { friends ->
                val adapter = CustomUsersAdapter(this@FriendsActivity, friends.toMutableList(), this@FriendsActivity, this@FriendsActivity, true)
                adapter.setItemClickedPrompt(getString(R.string.start_chat_with_selected_user))
                friendsRecyclerView.adapter = adapter
                (friendsRecyclerView.adapter as CustomUsersAdapter).notifyDataSetChanged()
            }
        }

        searchButtonImageButton.setOnClickListener {
            // disable button
            searchButtonImageButton.isEnabled = false

            val query = searchBarSearchView.query.toString()
            searchBarSearchView.onActionViewExpanded()
            searchBarSearchView.clearFocus()
            searchBarSearchView.setQuery(query, true)

            // re-enable button
            searchButtonImageButton.postDelayed({
                searchButtonImageButton.isEnabled = true
            }, 1000)
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        lifecycleScope.launch {
            viewModel.getFriendRequests { friendRequests ->
                val adapter = CustomFriendRequestAdapter(this@FriendsActivity, this@FriendsActivity, friendRequests.toMutableList())
                friendRequestsRecyclerView.adapter = adapter
                (friendRequestsRecyclerView.adapter as CustomFriendRequestAdapter).notifyDataSetChanged()
                val tempString = getString(R.string.friend_requests) + " " +adapter.itemCount
                friendRequestNumberTextView.text = tempString
            }

            viewModel.getFriends { friends ->
                val adapter = CustomUsersAdapter(this@FriendsActivity, friends.toMutableList(), this@FriendsActivity, this@FriendsActivity, true)
                adapter.setItemClickedPrompt(getString(R.string.start_chat_with_selected_user))
                friendsRecyclerView.adapter = adapter
                (friendsRecyclerView.adapter as CustomUsersAdapter).notifyDataSetChanged()
            }
        }
    }

    override fun onButtonClicked(position: Int) {
        lifecycleScope.launch {
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

    override fun onUserClickConfirmed(receiverUser: UserData) {
        viewModel.viewModelScope.launch {
            val newChat = viewModel.startNewChat(receiverUser)

            if (newChat != null) {
                val intent = Intent(this@FriendsActivity, ChatActivity::class.java)
                intent.putExtra("chatId", newChat.id)
                intent.putExtra("chatName", receiverUser.email)

                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            else {
                val intent = Intent(this@FriendsActivity, ChatActivity::class.java)
                val id = '-' + viewModel.generateIdFromEmails(receiverUser.email, authViewModel.getUserId())
                intent.putExtra("chatId", id)
                intent.putExtra("chatName", receiverUser.email)

                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    override fun onDeleteConfirmed(deletedUser: UserData, position: Int) {
        viewModel.viewModelScope.launch {
            viewModel.removeUserFromFriends(this@FriendsActivity, deletedUser)
            (binding.friendsRecyclerView.adapter as CustomUsersAdapter).removeItem(deletedUser)
            binding.friendsRecyclerView.adapter!!.notifyItemRemoved(position)
        }
    }
}