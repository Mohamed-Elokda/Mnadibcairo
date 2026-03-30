package com.example.myapplication.domin.model

data class PaymentItem(
    val id: Int,
    val customerName: String,
    val amount: Double,
    val date: String,
    val paymentType: String, // "نقدي", "فودافون كاش", "أنستا باي"
    val userId: Int
)