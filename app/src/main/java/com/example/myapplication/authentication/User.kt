package com.example.myapplication.authentication


data class User(
    val username: String,
    val email: String
) {
    constructor() : this("", "")
}