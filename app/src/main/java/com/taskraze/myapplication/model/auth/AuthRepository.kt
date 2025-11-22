package com.taskraze.myapplication.model.auth

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.taskraze.myapplication.model.calendar.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository {
    suspend fun fetchUserDetails(): UserData {
        val firestoreDB = FirebaseFirestore.getInstance()
        return withContext(Dispatchers.Main) {
            val docRef = firestoreDB.collection("registered_users")
                .document(Firebase.auth.currentUser?.email.toString())
            val documentSnapshot = docRef.get().await()
            val username = documentSnapshot.getString("username") ?: ""
            val userId = documentSnapshot.getString("email") ?: ""
            val email = documentSnapshot.getString("email") ?: ""
            UserData(userId, username, email)
        }
    }

}