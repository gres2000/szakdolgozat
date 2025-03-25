package com.taskraze.myapplication.model.calendar

import com.taskraze.myapplication.model.room_database.data_classes.User
import java.util.Date

data class CalendarData(
    val id: Long,
    val name: String,
    var sharedPeopleNumber: Int,
    val sharedPeople: MutableList<User>,
    val owner: User,
    val events: MutableList<EventData>,
    var lastUpdated: Date
) {
    // Default constructor
    constructor() : this(0,"", 0, mutableListOf(), User(), mutableListOf(), Date()) // Initialize properties with default values
}