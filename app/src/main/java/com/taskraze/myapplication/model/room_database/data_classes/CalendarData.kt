package com.taskraze.myapplication.model.room_database.data_classes

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "calendar_items")
data class CalendarData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    var sharedPeopleNumber: Int,
    //val sharedPeople: MutableList<UserData>,
    val owner: UserData,
    //val eventList: MutableList<EventData>,
    var lastUpdated: Date
) {
    // Secondary constructor to allow creation without specifying the ID
    constructor(
        name: String,
        sharedPeopleNumber: Int,
        //sharedPeople: MutableList<UserData>,
        owner: UserData,
        //eventList: MutableList<EventData>,
        lastUpdated: Date
    ) : this(
        id = 0,
        name = name,
        sharedPeopleNumber = sharedPeopleNumber,
        //sharedPeople = sharedPeople,
        owner = owner,
        //eventList = eventList,
        lastUpdated = lastUpdated
    )
}