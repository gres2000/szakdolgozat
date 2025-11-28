package com.taskraze.myapplication.view.friends

import AuthViewModelFactory
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.R
import com.taskraze.myapplication.databinding.FoundUsersActivityBinding
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.taskraze.myapplication.model.calendar.UserData
import com.taskraze.myapplication.viewmodel.MainViewModelFactory
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.launch

class FoundUsersActivity : AppCompatActivity() {

    private lateinit var binding: FoundUsersActivityBinding
    private lateinit var foundUsersRecyclerView: RecyclerView
    private lateinit var searchResultsTextView: TextView
    private lateinit var viewModel: MainViewModel
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FoundUsersActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        foundUsersRecyclerView = findViewById(R.id.recyclerViewFoundUsers)
        searchResultsTextView = findViewById(R.id.textViewSearchResults)
        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(this)
        )[AuthViewModel::class.java]

        val factory = MainViewModelFactory(authViewModel.getUserId(), authViewModel.loggedInUser.value!!)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        val searchQuery = intent.getStringExtra("searchQuery")

        val firestoreDB = FirebaseFirestore.getInstance()

        val searchResultString = getString(R.string.search_results) + " \"" + searchQuery + "\":"
        searchResultsTextView.text = searchResultString

        foundUsersRecyclerView.layoutManager = GridLayoutManager(this, 3)

        val dataList: MutableList<UserData> = mutableListOf()
        val searchQueryLower = searchQuery?.lowercase() ?: ""
        val usersRef = firestoreDB.collection("registered_users")

        usersRef.get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val user = doc.toObject(UserData::class.java) ?: continue

                    val usernameLower = user.username.lowercase()
                    val emailLower = user.email.lowercase()
                    if (usernameLower.contains(searchQueryLower) || emailLower.contains(searchQueryLower)) {
                        dataList.add(user)
                    }
                }

                viewModel.viewModelScope.launch {
                    viewModel.fetchUsersFromFriendsList { alreadyFriends ->

                        viewModel.getFriendRequests { friendRequests ->
                            val pendingReceiverIds = friendRequests
                                .filter { it.senderId == authViewModel.getUserId() && it.status == "pending" }
                                .map { it.receiverId }
                                .toSet()
                            Log.d("FoundUsersActivitytag", "Pending receiver IDs: $pendingReceiverIds")
                            dataList.removeAll(alreadyFriends)
                            dataList.remove(authViewModel.loggedInUser.value)
                            dataList.removeAll { pendingReceiverIds.contains(it.email) }
                            Log.d("FoundUsersActivitytag", "final receiver IDs: $dataList")

                            foundUsersRecyclerView.adapter =
                                CustomFoundUsersAdapter(this@FoundUsersActivity, dataList)
                            (foundUsersRecyclerView.adapter as CustomFoundUsersAdapter).notifyDataSetChanged()
                        }
                    }
                }
            }
    }
}