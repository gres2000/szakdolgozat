package com.example.myapplication.calendar

import android.os.Parcelable
import com.example.myapplication.authentication.User
import kotlinx.parcelize.Parcelize
import java.util.Date

data class MyCalendar(
    val name: String,
    val sharedPeopleNumber: Int,
    val sharedPeople: MutableList<User>,
    val owner: User,
    val events: MutableList<Event>,
    var lastUpdated: Date
)