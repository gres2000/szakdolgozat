package com.taskraze.myapplication.friends.data_classes

data class UserFriendsData(
    val userId: String,
    val friends: List<String>
) {
    constructor(): this("", listOf())
}