package com.example.myapplication.domin.model

data class ReturnedDetailsModel(
    val id: String,
    val returnedId: String,
    val itemId: Int,
    val itemName: String,
    var amount: Int,
    var price: Double,
)