package com.taskraze.myapplication.model.chat

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.taskraze.myapplication.model.auth.AuthRepository
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.UserData
import com.taskraze.myapplication.viewmodel.MainViewModel

class ChatRepository {
    private val authRepository = AuthRepository()
    suspend fun deleteUserFromRealtimeSharedChat(myUser: UserData, calendarData: CalendarData) {
        // authRepository.fetchUserDetails()

        val chatId = MainViewModel.generateIdFromOwner(calendarData.owner.email, calendarData.id.toString())

        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")

        val chatsRef = database.getReference("chats")
        val existingChatRef = chatsRef.child(chatId)

        //check if chat exists
        existingChatRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //chat exists
                    val chatsRef = database.getReference("chats")
                    val usersRef = chatsRef.child(chatId).child("users")

                    val newUserList = calendarData.sharedPeople.toMutableList()
                    newUserList.sortWith(compareByDescending { it.email })

                    newUserList.remove(myUser)
                    newUserList.add(calendarData.owner)
                    newUserList.sortWith(compareByDescending { it.email })
                    usersRef.setValue(newUserList).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val leaveMessage = FriendlyMessage(
                                "# # # # # #\n${myUser.email} has been removed from the chat\n# # # # # #",
                                "System"
                            )
                            val messagesRef = chatsRef.child(chatId).child("messages")
                            messagesRef.push().setValue(leaveMessage)
                        }
                    }
                } else {
                    // Chat does not exist
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // error
            }
        })
    }

    suspend fun addUserToRealtimeSharedChat(myUser: UserData, calendarData: CalendarData) {
        // authRepository.fetchUserDetails()

        val chatId = MainViewModel.generateIdFromOwner(calendarData.owner.email, calendarData.id.toString())

        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
        val chatsRef = database.getReference("chats")
        val existingChatRef = chatsRef.child(chatId)

        //check if chat exists
        existingChatRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //chat exists
                    val usersRef = chatsRef.child(chatId).child("users")
                    val newUserList = calendarData.sharedPeople.toMutableList()
                    newUserList.add(calendarData.owner)
                    newUserList.sortWith(compareByDescending { it.email })

                    usersRef.setValue(newUserList)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val leaveMessage = FriendlyMessage(
                                    "# # # # # #\n${myUser.email} has been added to the chat\n# # # # # #",
                                    "System"
                                )
                                val messagesRef = existingChatRef.child("messages")
                                messagesRef.push().setValue(leaveMessage)
                            } else {
                                //error
                            }
                        }
                } else {
                    // Chat does not exist
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // error
            }
        })
    }

}