package com.example.myapplication.calendar

import com.example.myapplication.authentication.User
import java.util.Date

data class Calendar(
    val name: String,
    val sharedPeopleNumber: Int,
    val sharedPeople: MutableList<User>,
    val owner: User,
    val events: MutableList<Event>,
    val lastUpdated: Date
)