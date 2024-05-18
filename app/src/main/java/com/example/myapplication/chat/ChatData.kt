package com.example.myapplication.chat

import com.example.myapplication.authentication.User

data class ChatData(
    var id: String,
    var title: String,
    val users: MutableList<User>,
    var messages: HashMap<String,FriendlyMessage>
) {
    constructor(): this("", "", mutableListOf(), hashMapOf())
}