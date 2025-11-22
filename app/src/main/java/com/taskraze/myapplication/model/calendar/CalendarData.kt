package com.taskraze.myapplication.model.calendar

import java.util.Date

data class CalendarData(
    val id: Long,
    val name: String,
    var sharedPeopleNumber: Int,
    val sharedPeople: MutableList<UserData>,
    val owner: UserData,
    val events: MutableList<EventData>,
    var lastUpdated: Date
) {
    // Default constructor
    constructor() : this(0,"", 0, mutableListOf(), UserData(), mutableListOf(), Date())
}