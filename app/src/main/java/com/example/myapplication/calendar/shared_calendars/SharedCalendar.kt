package com.example.myapplication.calendar.shared_calendars

import com.example.myapplication.calendar.calendar_details.MyCalendar

data class SharedCalendar(
    val email: String = "",
    val userId: String = "",
    val calendars: List<MyCalendar> = emptyList()
)