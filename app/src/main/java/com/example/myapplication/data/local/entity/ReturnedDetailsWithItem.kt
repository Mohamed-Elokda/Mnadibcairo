package com.example.myapplication.data.local.entity

import androidx.room.Embedded

data class ReturnedDetailsWithItem(
    @Embedded val details: ReturnedDetailsEntity,
    val itemName: String
)