package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "returnedDetails")
data class ReturnedDetailsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val returnedId:Int,
    val itemId:Int,
    val amount:Int,
    val price: Double
)
