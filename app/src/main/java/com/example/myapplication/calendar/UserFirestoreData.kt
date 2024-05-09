package com.example.myapplication.calendar

data class UserFirestoreData(
    val userId: String,
    val email: String,
    val calendars: List<MyCalendar>
){
    // Default constructor
    constructor() : this("", "", listOf()) // Initialize properties with default values
}