package com.example.myapplication.data.local.entity

data class ItemMovementProjection(
    val date: String,
    val transactionType: String,
    val documentNumber: String,
    val partyName: String?, // الحقل الجديد
    val qtyIn: Int,
    val qtyOut: Int
)