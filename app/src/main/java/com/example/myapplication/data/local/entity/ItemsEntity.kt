package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("Items")
data class ItemsEntity (
    @PrimaryKey(autoGenerate = false)
    val id: Int=0,
    val itemName: String,
    val itemNum:Int
)