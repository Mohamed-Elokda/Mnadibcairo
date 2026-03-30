package com.example.myapplication.domin.model

data class ReturnedDetailsModel(
    val id: Int,
    val returnedId: Int,
    val itemId: Int,
    val itemName: String,
    val amount: Int,
    val price: Double,
)