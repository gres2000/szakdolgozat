package com.taskraze.myapplication.view.recommendation

data class Recommendation(
    val id: Int,
    val title: String,
    val type: String,
    val tags: List<String>,
    val link: String? = null
)