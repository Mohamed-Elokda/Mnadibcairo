package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("Items")
data class ItemsEntity (
    @PrimaryKey(autoGenerate = false)
    val id: Int=0,
    val itemName: String,
    val itemNum:Int,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)