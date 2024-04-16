package com.example.myapplication.calendar

import java.util.Date

class Event(
    var title: String,
    var description: String? = null,
    var startTime: Date,
    var endTime: Date,
    var location: String? = null,
    var reminders: List<Reminder>? = null,
    var recurrenceRule: RecurrenceRule? = null,
    var visibility: Visibility = Visibility.PUBLIC,
    var organizer: String? = null
) {
    // Inner class representing a reminder
    data class Reminder(
        val timeBeforeEvent: Long,
        val method: ReminderMethod
    )

    // Enum class representing reminder methods
    enum class ReminderMethod {
        EMAIL,
        NOTIFICATION,
        POPUP
    }

    // Enum class representing event visibility
    enum class Visibility {
        PUBLIC,
        PRIVATE,
        CONFIDENTIAL
    }

    // Inner class representing recurrence rule
    data class RecurrenceRule(
        val frequency: Frequency,
        val interval: Int = 1,
        val endDate: Date? = null
    ) {
        enum class Frequency {
            DAILY,
            WEEKLY,
            MONTHLY,
            YEARLY
        }
    }

    override fun toString(): String {
        return "Event(title='$title', description=$description, startTime=$startTime, endTime=$endTime, location=$location, reminders=$reminders, recurrenceRule=$recurrenceRule, visibility=$visibility, organizer=$organizer)"
    }
}