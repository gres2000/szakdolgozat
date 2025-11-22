package com.taskraze.myapplication.model.calendar

data class UserData(
    val userId: String,
    val username: String,
    val email: String,
){
    // Default constructor
    constructor() : this("", "","") // Initialize properties with default values
}