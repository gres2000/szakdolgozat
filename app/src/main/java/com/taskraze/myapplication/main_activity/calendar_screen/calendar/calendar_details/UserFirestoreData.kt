package com.taskraze.myapplication.main_activity.calendar_screen.calendar.calendar_details

data class UserFirestoreData(
    val userId: String,
    val email: String,
    val calendars: MutableList<MyCalendar>
){
    // Default constructor
    constructor() : this("", "", mutableListOf()) // Initialize properties with default values
}