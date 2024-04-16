package com.example.myapplication.calendar

import java.util.Date

class Calendar(
    var name: String,
    private val events: MutableList<Event> = mutableListOf()
) {
    fun addEvent(event: Event) {
        events.add(event)
    }

    fun removeEvent(event: Event) {
        events.remove(event)
    }

    fun getEvents(): List<Event> {
        return events.toList()
    }

    fun clearEvents() {
        events.clear()
    }

    override fun toString(): String {
        return "Calendar(name='$name', events=$events)"
    }
}