package com.example.myapplication.domin.model

import androidx.room.PrimaryKey

data class Outbound (

    val id:Int,
    val userId: String,
    val customerId: Int,
    val customerName: String,
    val invorseNumber: Int,
    val image: String,
    val outboundDate: String,
    val latitude: Double=0.0,
    val longitude: Double=0.0,
    val moneyResive: Int,
    val isSynced: Boolean

)