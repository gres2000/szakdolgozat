package com.example.myapplication.authentication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


data class User(
    val username: String,
    val emailAddress: String
) {
    constructor() : this("", "")
}