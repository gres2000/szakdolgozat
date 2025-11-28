package com.taskraze.myapplication.model.calendar

data class UserData(
    val userId: String,
    val username: String,
    val email: String,
){
    constructor() : this("", "","")
}