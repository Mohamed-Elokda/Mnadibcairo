package com.example.myapplication.domin.model

import androidx.room.Relation

class ReturnedWithNameModel
    (
    val returnedModel: ReturnedModel,
    val customerName: String,
    val totalPrice: Double,

    val itemName: String) {
}