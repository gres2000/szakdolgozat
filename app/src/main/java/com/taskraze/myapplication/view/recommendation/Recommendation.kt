package com.taskraze.myapplication.view.recommendation

data class Recommendation(
    val id: Int = 0,
    val title: String = "",
    val type: String = "",
    val tags: List<String> = emptyList(),
    val url: String? = null
)