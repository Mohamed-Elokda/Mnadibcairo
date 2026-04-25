package com.example.myapplication.domin.model

data class TransferDetails(
    val id: String = java.util.UUID.randomUUID().toString(),
    val transferId: String,
    val itemId: Int,
    val amount: Int
)