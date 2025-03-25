package com.taskraze.myapplication.model.todo

data class TaskData(
    var taskId: Int,
    var title: String,
    var description: String?,
    var time: String?,
    var isChecked: Boolean
)