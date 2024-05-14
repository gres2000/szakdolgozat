package com.example.myapplication.friends

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.widget.SearchView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.FriendsActivityBinding
import com.example.myapplication.viewModel.MainViewModel
import kotlinx.coroutines.launch

class FriendsActivity : AppCompatActivity(), CustomFriendRequestAdapter.OnAcceptButtonClickedListener {

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
            viewModel.authenticateUser()
        }
        // Initialize your views or perform any other setup here
        searchBarSearchView.setOnQueryTextListener(searchViewQueryListener)

        friendRequestsRecyclerView.layoutManager = LinearLayoutManager(this)
        friendsRecyclerView.layoutManager = GridLayoutManager(this, 3)

        lifecycleScope.launch {
            viewModel.loggedInUser = viewModel.loggedInDeferred.await()
            viewModel.getFriendRequests { friendRequests ->
                val adapter = CustomFriendRequestAdapter(this@FriendsActivity, this@FriendsActivity, friendRequests.toMutableList())
                friendRequestsRecyclerView.adapter = adapter
                (friendRequestsRecyclerView.adapter as CustomFriendRequestAdapter).notifyDataSetChanged()
                val tempString = getString(R.string.friend_requests) + " " +adapter.itemCount
                friendRequestNumberTextView.text = tempString
            }
        }

        lifecycleScope.launch {
            viewModel.loggedInUser = viewModel.loggedInDeferred.await()
            viewModel.getFriends { friends ->
                val adapter = CustomFriendsAdapter(this@FriendsActivity, friends.toMutableList())
                friendsRecyclerView.adapter = adapter
                (friendsRecyclerView.adapter as CustomFriendsAdapter).notifyDataSetChanged()
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
            viewModel.loggedInUser = viewModel.loggedInDeferred.await()
            viewModel.getFriends { friends ->
                val adapter = CustomFriendsAdapter(this@FriendsActivity, friends.toMutableList())
                friendsRecyclerView.adapter = adapter
                (friendsRecyclerView.adapter as CustomFriendsAdapter).notifyDataSetChanged()

                val tempString = getString(R.string.friend_requests) + " " + (friendRequestNumberTextView.text.last().digitToInt() - 1)
                friendRequestNumberTextView.text = tempString
            }
        }
    }

    // You can override other Activity lifecycle methods as needed
}