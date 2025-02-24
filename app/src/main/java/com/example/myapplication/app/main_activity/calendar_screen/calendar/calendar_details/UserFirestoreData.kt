package com.example.myapplication.app.main_activity.calendar_screen.calendar.calendar_details

import com.example.myapplication.app.main_activity.calendar_screen.calendar.calendar_details.MyCalendar

data class UserFirestoreData(
    val userId: String,
    val email: String,
    val calendars: MutableList<MyCalendar>
){
    // Default constructor
    constructor() : this("", "", mutableListOf()) // Initialize properties with default values
}