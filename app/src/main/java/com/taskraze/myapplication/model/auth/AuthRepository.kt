package com.taskraze.myapplication.model.auth

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.taskraze.myapplication.model.room_database.data_classes.User
import com.taskraze.myapplication.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository {
    suspend fun fetchUserDetails(): User {
        val firestoreDB = FirebaseFirestore.getInstance()
        return withContext(Dispatchers.Main) {
            val docRef = firestoreDB.collection("registered_users")
                .document(Firebase.auth.currentUser?.email.toString())
            val documentSnapshot = docRef.get().await()
            val username = documentSnapshot.getString("username") ?: ""
            val emailAddress = documentSnapshot.getString("email") ?: ""
            User(username, emailAddress)
        }
    }

}