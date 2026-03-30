package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("Customer")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val userId: String,
    val customerName: String,
    val CustomerNum: Int,
    val customerDebt: Double,

    val isSync: Boolean

)
