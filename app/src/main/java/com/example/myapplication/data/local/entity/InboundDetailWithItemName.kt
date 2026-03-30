package com.example.myapplication.data.local.entity

data class InboundDetailWithItemName(
    val itemId: String,
    val itemName: String, // الاسم اللي هنحتاجه
    val quantity: Int,
    val price: Double
)