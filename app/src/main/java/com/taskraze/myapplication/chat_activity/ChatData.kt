package com.taskraze.myapplication.chat_activity

import com.taskraze.myapplication.authentication_activity.User

data class ChatData(
    var id: String,
    var title: String,
    val users: MutableList<User>,
    var messages: HashMap<String, FriendlyMessage>
) {
    constructor(): this("", "", mutableListOf(), hashMapOf())
}