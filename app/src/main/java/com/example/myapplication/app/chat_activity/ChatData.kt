package com.example.myapplication.app.chat_activity

import com.example.myapplication.app.authentication_activity.User

data class ChatData(
    var id: String,
    var title: String,
    val users: MutableList<User>,
    var messages: HashMap<String, FriendlyMessage>
) {
    constructor(): this("", "", mutableListOf(), hashMapOf())
}