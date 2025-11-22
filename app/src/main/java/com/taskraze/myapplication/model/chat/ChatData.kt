package com.taskraze.myapplication.model.chat

import com.taskraze.myapplication.model.calendar.UserData

data class ChatData(
    var id: String,
    var title: String,
    val users: MutableList<UserData>,
    var messages: HashMap<String, FriendlyMessage>
) {
    constructor(): this("", "", mutableListOf(), hashMapOf())
}