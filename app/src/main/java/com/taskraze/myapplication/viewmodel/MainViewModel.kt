package com.taskraze.myapplication.viewmodel

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.taskraze.myapplication.model.calendar.UserData
import com.taskraze.myapplication.model.chat.ChatData
import com.taskraze.myapplication.model.chat.FriendlyMessage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.model.calendar.UserCalendarsData
import com.taskraze.myapplication.model.friends.FriendRequestData
import com.taskraze.myapplication.model.friends.UserFriendsData
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Calendar

class MainViewModel(private val userId: String, val userData: UserData) : ViewModel() {
    private val firestoreDB = FirebaseFirestore.getInstance()
    var auth = Firebase.auth
    private var _CalendarDataToPass: CalendarData? = null
    var newEventStartingDay: Calendar? = null

    fun passCalendarToFragment(calendarData: CalendarData) {
        _CalendarDataToPass = calendarData
    }

    fun getCalendarToFragment(): CalendarData? {
        val temp = _CalendarDataToPass
        _CalendarDataToPass = null
        return temp
    }

    fun getFriendRequests(callback: (List<FriendRequestData>) -> Unit) {
        val receiverQuery = firestoreDB.collection("friend_requests")
            .whereEqualTo("receiverId", userId)

        val senderQuery = firestoreDB.collection("friend_requests")
            .whereEqualTo("senderId", userId)

        val friendRequestDataList = mutableListOf<FriendRequestData>()
        receiverQuery.get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val receiverId = document.getString("receiverId") ?: ""
                    val senderId = document.getString("senderId") ?: ""
                    val status = document.getString("status") ?: ""
                    val friendRequestData = FriendRequestData(receiverId, senderId, status)
                    if (status == "pending") {
                        friendRequestDataList.add(friendRequestData)
                    }
                }

                senderQuery.get().addOnSuccessListener { result2 ->
                    for (document in result2) {
                        val receiverId = document.getString("receiverId") ?: ""
                        val senderId = document.getString("senderId") ?: ""
                        val status = document.getString("status") ?: ""
                        val friendRequestData = FriendRequestData(receiverId, senderId, status)
                        if (status == "pending") {
                            friendRequestDataList.add(friendRequestData)
                        }
                    }

                    callback(friendRequestDataList)
                }

            }
            .addOnFailureListener { _ ->
                Log.d(TAG, "failed to retrieve friend requests")
            }
    }

    fun handleFriendRequest(choice: String, friendRequestData: FriendRequestData, callback: (Boolean) -> Unit) {
        val friendRequestsCollection = firestoreDB.collection("friend_requests")

        friendRequestsCollection
            .whereEqualTo("senderId", friendRequestData.senderId)
            .whereEqualTo("receiverId", auth.currentUser?.email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    document.reference.update("status", choice)
                        .addOnSuccessListener {
                            callback(true)
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun getFriends(callback: (MutableList<UserData>) -> Unit) {
        firestoreDB.collection("friend_requests")
            .whereEqualTo("receiverId", userId)
            .get()
            .addOnSuccessListener { result ->
                val acceptedFriends = mutableListOf<FriendRequestData>()
                val rejectedFriends = mutableListOf<FriendRequestData>()
                for (document in result) {
                    val receiverId = document.getString("receiverId") ?: ""
                    val senderId = document.getString("senderId") ?: ""
                    val status = document.getString("status") ?: ""
                    val friendRequestData = FriendRequestData(receiverId, senderId, status)
                    if (status == "accepted") {
                        acceptedFriends.add(friendRequestData)
                    } else if (status == "rejected") {
                        rejectedFriends.add(friendRequestData)
                    }
                }
                viewModelScope.launch(Dispatchers.IO) {
                    updateOrCreateUserFriendsDocument(acceptedFriends).await()
                    deleteRejectedFriendRequests(rejectedFriends)
                    deleteAcceptedFriendRequests(acceptedFriends)

                    fetchUsersFromFriendsList(callback)
                }

            }
            .addOnFailureListener { _ ->
                Log.d(TAG, "failed to retrieve friend requests")
            }
    }

    fun fetchUsersFromFriendsList(callback: (MutableList<UserData>) -> Unit) {
        firestoreDB.collection("user_friends")
            .document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val friendsList = documentSnapshot.toObject(UserFriendsData::class.java)?.friends ?: emptyList()

                val users = mutableListOf<UserData>()
                val userRef = firestoreDB.collection("registered_users")

                val queries = friendsList.map { friendId ->
                    userRef.document(friendId).get()
                }

                Tasks.whenAllSuccess<DocumentSnapshot>(queries)
                    .addOnSuccessListener { snapshots ->
                        snapshots.forEach { snapshot ->
                            val user = snapshot.toObject(UserData::class.java)
                            user?.let {
                                users.add(user)
                            }
                        }
                        callback(users)
                    }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error fetching user data for friends", exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error fetching user friends document", exception)
            }
    }

    private fun updateOrCreateUserFriendsDocument(acceptedFriends: List<FriendRequestData>): Deferred<Unit> {
        val currentId = userId
        val deferred = CompletableDeferred<Unit>()

        firestoreDB.collection("user_friends")
            .document(currentId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val currentUserFriendsData = documentSnapshot.toObject(UserFriendsData::class.java)
                val existingFriends = currentUserFriendsData?.friends ?: emptyList()

                val updatedFriends = existingFriends.toMutableList()
                acceptedFriends.forEach { friendRequest ->
                    if (!existingFriends.contains(friendRequest.senderId)) {
                        updatedFriends.add(friendRequest.senderId)
                    }
                }

                val userFriendsMap = mapOf(
                    "userId" to currentId,
                    "friends" to updatedFriends
                )
                firestoreDB.collection("user_friends")
                    .document(currentId)
                    .set(userFriendsMap)
                    .addOnSuccessListener {
                        Log.d(TAG, "User friends document updated successfully.")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error updating user friends document", e)
                    }

                for (friendRequest in acceptedFriends) {
                    val friendEmail = friendRequest.senderId
                    firestoreDB.collection("user_friends")
                        .document(friendEmail)
                        .get()
                        .addOnSuccessListener { documentSnapshot ->
                            val friendFriends = documentSnapshot.toObject(UserFriendsData::class.java)
                            val updatedFriends = friendFriends?.friends?.toMutableList() ?: mutableListOf()
                            if (!updatedFriends.contains(userId)) {
                                updatedFriends.add(userId)
                                val data = mapOf("friends" to updatedFriends)
                                firestoreDB.collection("user_friends")
                                    .document(friendEmail)
                                    .set(data, SetOptions.merge())
                                    .addOnSuccessListener {
                                        deferred.complete(Unit)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w(TAG, "Error updating user $friendEmail friends document", e)
                                        deferred.completeExceptionally(e)
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error getting user $friendEmail friends document", e)
                            deferred.completeExceptionally(e)
                        }
                }
                deferred.complete(Unit)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user friends document", e)
                deferred.completeExceptionally(e)
            }
        return deferred
    }

    private fun deleteRejectedFriendRequests(rejectedFriends: List<FriendRequestData>) {
        val batch = firestoreDB.batch()

        rejectedFriends.forEach { friendRequest ->
            val query = firestoreDB.collection("friend_requests")
                .whereEqualTo("receiverId", friendRequest.receiverId)
                .whereEqualTo("senderId", friendRequest.senderId)
                .whereEqualTo("status", friendRequest.status)

            query.get()
                .addOnSuccessListener { snapshot ->
                    snapshot.forEach { document ->
                        batch.delete(document.reference)
                    }

                    batch.commit()
                        .addOnSuccessListener {
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error deleting rejected friend requests", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting rejected friend requests to delete", e)
                }
        }
    }

    private fun deleteAcceptedFriendRequests(acceptedFriends: List<FriendRequestData>) {
        val batch = firestoreDB.batch()

        acceptedFriends.forEach { friendRequest ->
            val query = firestoreDB.collection("friend_requests")
                .whereEqualTo("receiverId", friendRequest.receiverId)
                .whereEqualTo("senderId", friendRequest.senderId)
                .whereEqualTo("status", friendRequest.status)

            query.get()
                .addOnSuccessListener { snapshot ->
                    snapshot.forEach { document ->
                        batch.delete(document.reference)
                    }

                    // Commit the batch delete operation
                    batch.commit()
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error deleting accepted friend requests", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting accepted friend requests to delete", e)
                }
        }
    }

    fun generateIdFromEmails(receiverUserEmail: String, senderUserEmail: String): String {
        val sortedStrings = listOf(receiverUserEmail, senderUserEmail).sorted()

        val combinedString = sortedStrings.joinToString(separator = "")

        val digest = MessageDigest.getInstance("SHA-256")

        val hashedBytes = digest.digest(combinedString.toByteArray())

        val stringBuilder = StringBuilder()
        for (byte in hashedBytes) {
            stringBuilder.append(String.format("%02x", byte))
        }

        return stringBuilder.toString()
    }

    private fun generateIdFromOwner(ownerEmail: String, calendarId: String): String {
        val combinedString = ownerEmail + calendarId

        val digest = MessageDigest.getInstance("SHA-256")

        val hashedBytes = digest.digest(combinedString.toByteArray())

        val stringBuilder = StringBuilder()
        for (byte in hashedBytes) {
            stringBuilder.append(String.format("%02x", byte))
        }

        return stringBuilder.toString()
    }

    private suspend fun checkExistingChat(userId1: String, userId2: String): Boolean {
        val chatId = '-' + generateIdFromEmails(userId1, userId2)

        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
        val chatsRef = database.getReference("chats")

        val chatSnapshot = chatsRef.child(chatId).get().await()
        return chatSnapshot.exists()
    }

    suspend fun startNewChat(chosenFriend: UserData): ChatData? {
        val existingChat = checkExistingChat(userId, chosenFriend.userId)
        if (existingChat) {
            return null
        }

        val newChat = ChatData()
        val chatId = "-" + generateIdFromEmails(userId, chosenFriend.email)
        newChat.id = chatId
        newChat.title = chosenFriend.email + "&" + userId
        newChat.users.add(userData)
        newChat.users.add(chosenFriend)
        newChat.users.sortWith(compareByDescending { it.email })

        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
        val chatsRef = database.getReference("chats")
        val chatRef = chatsRef.child(chatId)


        chatRef.setValue(newChat)
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving chat", e)
            }

        chatRef.child("messages").push().setValue(FriendlyMessage("This is the start of your chat.", "", ""))

        return newChat
    }

    fun quitChat(context: Context, chatData: ChatData) {
        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
        val chatsRef = database.getReference("chats")
        val usersRef = chatsRef.child(chatData.id).child("users")

        val currentUserEmail = userId

        val index = chatData.users.indexOfFirst { it.email == currentUserEmail }

        if (index == -1) {
            Toast.makeText(context, "You are not in this chat.", Toast.LENGTH_SHORT).show()
            return
        }

        chatData.users.removeAt(index)

        usersRef.setValue(chatData.users).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Quit chat successfully", Toast.LENGTH_SHORT).show()

                val leaveMessage = FriendlyMessage(
                    "# # # # # #\n${currentUserEmail} has left the chat\n# # # # # #",
                    "System"
                )

                val messagesRef = chatsRef.child(chatData.id).child("messages")
                messagesRef.push().setValue(leaveMessage)

                if (chatData.users.isEmpty()) {
                    chatsRef.child(chatData.id).removeValue()
                }
            }
        }
    }

    fun removeUserFromFriends(context: Context, deletedUser: UserData) {
        firestoreDB.collection("user_friends")
            .document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                //current user
                val currentUserFriendsData = documentSnapshot.toObject(UserFriendsData::class.java)
                val existingFriends = currentUserFriendsData?.friends ?: emptyList()

                val newFriendsList = existingFriends.toMutableList()
                newFriendsList.remove(deletedUser.email)
                firestoreDB.collection("user_friends")
                    .document(userId).update("friends", newFriendsList)

                //other user
                firestoreDB.collection("user_friends")
                    .document(deletedUser.email)
                    .get()
                    .addOnSuccessListener { OtherDocumentSnapshot ->
                        val otherUserFriendsData = OtherDocumentSnapshot.toObject(UserFriendsData::class.java)
                        val otherExistingFriends = otherUserFriendsData?.friends ?: emptyList()

                        val otherNewFriendsList = otherExistingFriends.toMutableList()
                        otherNewFriendsList.remove(userId)

                        firestoreDB.collection("user_friends")
                            .document(deletedUser.email).update("friends", otherNewFriendsList)

                    }
                    .addOnFailureListener { _ ->
                        Toast.makeText(context, "Could not delete user from friends", Toast.LENGTH_SHORT).show()
                    }

            }
            .addOnFailureListener { _ ->
                Toast.makeText(context, "Could not delete user from friends", Toast.LENGTH_SHORT).show()
            }
    }

    suspend fun startGroupChat(calendar: CalendarData): ChatData? {

        val chatId = generateIdFromOwner(calendar.owner.email, calendar.id.toString())
        val database = FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
        val chatsRef = database.getReference("chats")
        val chatRef = chatsRef.child(chatId)

        val loggedInUser = userData
        if (loggedInUser == null) {
            Log.e(TAG, "Logged in user is not valid. Cannot start group chat.")
            return null
        }

        val existingChatSnapshot = chatRef.get().await()
        if (existingChatSnapshot.exists()) {
            val existingChat = existingChatSnapshot.getValue(ChatData::class.java) ?: return null

            val userExists = existingChat.users.any { it.email == loggedInUser.email }

            if (!userExists) {
                existingChat.users.add(loggedInUser)
                existingChat.users.sortWith(compareByDescending { it.email })

                chatRef.setValue(existingChat)
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating existing chat", e)
                    }
            }

            return existingChat
        }

        Log.d(TAG, "Creating new chat: $chatId")

        val newChat = ChatData().apply {
            id = chatId
            title = calendar.name
            users.add(loggedInUser)
            for (user in calendar.sharedPeople) {
                users.add(user)
            }
            users.sortWith(compareByDescending { it.email })
        }

        chatRef.setValue(newChat)
            .addOnSuccessListener {
                Log.d(TAG, "New chat created: $chatId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving chat", e)
            }

        chatRef.child("messages")
            .push()
            .setValue(FriendlyMessage("This is the start of your chat.", "", ""))

        return newChat
    }

}