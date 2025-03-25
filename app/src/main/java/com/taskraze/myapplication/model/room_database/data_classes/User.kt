package com.taskraze.myapplication.model.room_database.data_classes


data class User(
    val username: String,
    val email: String
) {
    constructor() : this("", "")
}