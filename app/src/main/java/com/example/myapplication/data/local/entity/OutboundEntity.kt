package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Outbound")
data class OutboundEntity (
    @PrimaryKey(autoGenerate = true)
    val id:Int,
    val userId: String,
    val customerId: Int,
    val invorseNumber: Int,
    val image: String,
    val outboundDate: String,
    val latitude: Double=0.0,
    val longitude: Double=0.0,
    val moneyResive: Int,
    val isSynced: Boolean

)