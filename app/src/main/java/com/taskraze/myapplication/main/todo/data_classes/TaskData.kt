package com.taskraze.myapplication.main.todo.data_classes

data class TaskData(
    var taskId: Int,
    var title: String,
    var description: String?,
    var time: String?,
    var isChecked: Boolean
)