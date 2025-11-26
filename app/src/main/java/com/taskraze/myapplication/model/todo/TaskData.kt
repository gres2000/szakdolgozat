package com.taskraze.myapplication.model.todo

import java.util.UUID

data class TaskData(
    var taskId: String = UUID.randomUUID().toString(),
    var title: String,
    var description: String?,
    var time: String?,
    var isChecked: Boolean
) {
    constructor() : this(UUID.randomUUID().toString(), "", null, null, false)
}