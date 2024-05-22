package com.example.myapplication.calendar.calendar_details

import com.example.myapplication.calendar.calendar_details.MyCalendar

data class UserFirestoreData(
    val userId: String,
    val email: String,
    val calendars: MutableList<MyCalendar>
){
    // Default constructor
    constructor() : this("", "", mutableListOf()) // Initialize properties with default values
}