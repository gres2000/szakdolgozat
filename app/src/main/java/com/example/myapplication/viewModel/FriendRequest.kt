package com.example.myapplication.viewModel

import java.util.Date

data class FriendRequest (
    var receiverId: String,
    var senderId: String,
    var status: String
){
    constructor() : this("", "","")
}