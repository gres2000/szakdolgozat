package com.example.myapplication.app.view_model

data class UserFriendsData(
    val userId: String,
    val friends: List<String>
) {
    constructor(): this("", listOf())
}