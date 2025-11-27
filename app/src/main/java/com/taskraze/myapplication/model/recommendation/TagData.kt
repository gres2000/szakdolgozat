package com.taskraze.myapplication.model.recommendation

data class TagData(
    val tag: String = "",
    val weight: Int = 1,
    val refId: String = "NO_ID"
)
