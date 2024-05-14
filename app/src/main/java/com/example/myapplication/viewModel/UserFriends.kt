package com.example.myapplication.viewModel

data class UserFriends(
    val userId: String,
    val friends: List<String>
) {
    constructor(): this("", listOf())
}