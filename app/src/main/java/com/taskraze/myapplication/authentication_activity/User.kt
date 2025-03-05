package com.taskraze.myapplication.authentication_activity


data class User(
    val username: String,
    val email: String
) {
    constructor() : this("", "")
}