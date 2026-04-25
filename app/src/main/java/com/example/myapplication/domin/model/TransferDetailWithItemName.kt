package com.example.myapplication.domin.model

data class TransferDetailWithItemName(
    val id: String,
    val transferId: String,
    val itemId: Int,
    val itemName: String,
    val quantity: Double,
    val unitName: String? = null
)