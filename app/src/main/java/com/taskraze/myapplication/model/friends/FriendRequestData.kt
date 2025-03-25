package com.taskraze.myapplication.model.friends

data class FriendRequestData (
    var receiverId: String,
    var senderId: String,
    var status: String
){
    constructor() : this("", "","")
}