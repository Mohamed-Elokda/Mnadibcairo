package com.example.myapplication.domin.model

data class ItemMovement(
    val date: String,
    val transactionType: String,
    val documentNumber: String,
    val partyName: String?, // الحقل الجديد
    val qtyIn: Int,
    val qtyOut: Int,
    var runningStock: Int = 0
)