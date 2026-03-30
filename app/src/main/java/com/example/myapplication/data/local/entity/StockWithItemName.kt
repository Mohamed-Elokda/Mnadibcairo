package com.example.myapplication.data.local.entity

data class StockWithItemName(
    val id: Int,
    val ItemId: Int,
    val itemName: String, // الاسم اللي جاي من الـ Join
    val userId: String, // الاسم اللي جاي من الـ Join
    val CurrentAmount: Int,
    val fristDate: String
)