package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("Supplied")
data class Supplied (
    @PrimaryKey(autoGenerate = true)
    val id:Int,
    val suppliedName: String,
    val num: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)