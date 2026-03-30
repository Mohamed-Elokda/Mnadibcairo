package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("Supplied")
data class Supplied (
    @PrimaryKey(autoGenerate = true)
    val id:Int,
    val suppliedName: String,
    val num: String,
)