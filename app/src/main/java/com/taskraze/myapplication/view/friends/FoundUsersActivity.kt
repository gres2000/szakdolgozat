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
    private val TAG = "FoundUsersActivity"

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
        val dataList:MutableList<UserData> = mutableListOf()

        val searchQuery = intent.getStringExtra("searchQuery")

        val firestoreDB = FirebaseFirestore.getInstance()

        val usersRef  = firestoreDB.collection("registered_users")
        val searchResultString = getString(R.string.search_results) + " \"" + searchQuery + "\":"
        searchResultsTextView.text = searchResultString


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
                        val user = document.toObject(UserData::class.java)
                        dataList.add(user!!)
                    }
                    viewModel.viewModelScope.launch {
                        // authRepository.fetchUserDetails()
                        viewModel.fetchUsersFromFriendsList { alreadyFriends ->
                            for (data in dataList) {
                                if (alreadyFriends.contains(data)) {
                                    dataList.remove(data)
                                }
                            }
                            if (dataList.contains(authViewModel.loggedInUser.value)) {
                                dataList.remove(authViewModel.loggedInUser.value)
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
                        val user = document.toObject(UserData::class.java)
                        dataList.add(user!!)
                    }
                    viewModel.viewModelScope.launch {
                        viewModel.fetchUsersFromFriendsList { alreadyFriends ->
                            for (data in dataList) {
                                if (alreadyFriends.contains(data)) {
                                    dataList.remove(data)
                                }
                            }
                            if (dataList.contains(authViewModel.loggedInUser.value)) {
                                dataList.remove(authViewModel.loggedInUser.value)
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