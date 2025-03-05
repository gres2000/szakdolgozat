package com.taskraze.myapplication.main_activity.calendar_screen.calendar.calendar_details

import com.taskraze.myapplication.authentication_activity.User
import java.util.Date

data class MyCalendar(
    val id: Long,
    val name: String,
    var sharedPeopleNumber: Int,
    val sharedPeople: MutableList<User>,
    val owner: User,
    val events: MutableList<MyEvent>,
    var lastUpdated: Date
) {
    // Default constructor
    constructor() : this(0,"", 0, mutableListOf(), User(), mutableListOf(), Date()) // Initialize properties with default values
}