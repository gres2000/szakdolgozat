package com.taskraze.myapplication.model.calendar

import com.taskraze.myapplication.model.calendar.CalendarData

data class UserFirestoreData(
    val userId: String,
    val email: String,
    val calendars: MutableList<CalendarData>
){
    // Default constructor
    constructor() : this("", "", mutableListOf()) // Initialize properties with default values
}