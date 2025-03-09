package com.taskraze.myapplication.chat.data_classes

import com.taskraze.myapplication.room_database.data_classes.User

data class ChatData(
    var id: String,
    var title: String,
    val users: MutableList<User>,
    var messages: HashMap<String, FriendlyMessage>
) {
    constructor(): this("", "", mutableListOf(), hashMapOf())
}