package com.taskraze.myapplication.model.todo

import java.util.UUID

data class TaskData(
    var taskId: String = UUID.randomUUID().toString(),
    var title: String = "",
    var description: String? = null,
    var time: String? = null,
    var isChecked: Boolean = false,
    var notificationMinutesBefore: Int? = null
)