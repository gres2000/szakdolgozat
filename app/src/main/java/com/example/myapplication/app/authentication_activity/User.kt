package com.example.myapplication.app.authentication_activity


data class User(
    val username: String,
    val email: String
) {
    constructor() : this("", "")
}