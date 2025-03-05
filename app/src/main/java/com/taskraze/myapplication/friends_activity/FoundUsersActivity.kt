package com.taskraze.myapplication.friends_activity

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.R
import com.taskraze.myapplication.authentication_activity.User
import com.taskraze.myapplication.databinding.FoundUsersActivityBinding
import com.taskraze.myapplication.view_model.MainViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class FoundUsersActivity : AppCompatActivity() {

    private lateinit var binding: FoundUsersActivityBinding
    private lateinit var foundUsersRecyclerView: RecyclerView
    private lateinit var searchResutltsTextView: TextView
    private lateinit var viewModel: MainViewModel
    private val TAG = "FoundUsersActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FoundUsersActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        foundUsersRecyclerView = findViewById(R.id.recyclerViewFoundUsers)
        searchResutltsTextView = findViewById(R.id.textViewSearchResults)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        val dataList:MutableList<User> = mutableListOf()

        val searchQuery = intent.getStringExtra("searchQuery")

        val firestoreDB = FirebaseFirestore.getInstance()

        val usersRef  = firestoreDB.collection("registered_users")
        val searchResultString = getString(R.string.search_results) + " \"" + searchQuery + "\":"
        searchResutltsTextView.text = searchResultString


        foundUsersRecyclerView.layoutManager = GridLayoutManager(this, 3)
        if (searchQuery != null && searchQuery.contains("@")) {
            usersRef
                .orderBy("email")
                .startAt(searchQuery)
                .endAt(searchQuery + "\uf8ff")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        // Access the user data from the document
                        val user = document.toObject(User::class.java)
                        dataList.add(user!!)
                    }
                    viewModel.viewModelScope.launch {
                        viewModel.authenticateUser()
                        viewModel.fetchUsersFromFriendsList { alreadyFriends ->
                            for (data in dataList) {
                                if (alreadyFriends.contains(data)) {
                                    dataList.remove(data)
                                }
                            }
                            if (dataList.contains(viewModel.loggedInUser)) {
                                dataList.remove(viewModel.loggedInUser)
                            }

                            foundUsersRecyclerView.adapter = CustomFoundUsersAdapter(this@FoundUsersActivity, dataList)
                            (foundUsersRecyclerView.adapter as CustomFoundUsersAdapter).notifyDataSetChanged()
                        }
                    }
//                    foundUsersRecyclerView.adapter = CustomFoundUsersAdapter(this, dataList)
//                    (foundUsersRecyclerView.adapter as CustomFoundUsersAdapter).notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    // Handle any errors
                    Log.e(TAG, "Error searching users: $exception")
                }
        }
        else {
            usersRef
                .orderBy("username")
                .startAt(searchQuery)
                .endAt(searchQuery + "\uf8ff")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        // Access the user data from the document
                        val user = document.toObject(User::class.java)
                        dataList.add(user!!)
                    }
                    viewModel.viewModelScope.launch {
                        viewModel.fetchUsersFromFriendsList { alreadyFriends ->
                            for (data in dataList) {
                                if (alreadyFriends.contains(data)) {
                                    dataList.remove(data)
                                }
                            }
                            if (dataList.contains(viewModel.loggedInUser)) {
                                dataList.remove(viewModel.loggedInUser)
                            }

                            foundUsersRecyclerView.adapter = CustomFoundUsersAdapter(this@FoundUsersActivity, dataList)
                            (foundUsersRecyclerView.adapter as CustomFoundUsersAdapter).notifyDataSetChanged()
                        }
                    }
//                    foundUsersRecyclerView.adapter = CustomFoundUsersAdapter(this, dataList)
//                    (foundUsersRecyclerView.adapter as CustomFoundUsersAdapter).notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    // Handle any errors
                    Log.e(TAG, "Error searching users: $exception")
                }
        }
    }
}