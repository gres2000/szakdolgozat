package com.example.myapplication.app.view_model

data class FriendRequestData (
    var receiverId: String,
    var senderId: String,
    var status: String
){
    constructor() : this("", "","")
}