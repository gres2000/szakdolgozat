package com.taskraze.myapplication.model.friends

data class UserFriendsData(
    val userId: String,
    val friends: List<String>
) {
    constructor(): this("", listOf())
}