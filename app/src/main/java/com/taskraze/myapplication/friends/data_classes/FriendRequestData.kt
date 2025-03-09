package com.taskraze.myapplication.friends.data_classes

data class FriendRequestData (
    var receiverId: String,
    var senderId: String,
    var status: String
){
    constructor() : this("", "","")
}