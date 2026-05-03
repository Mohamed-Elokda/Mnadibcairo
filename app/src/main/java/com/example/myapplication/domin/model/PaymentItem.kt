package com.example.myapplication.domin.model

data class PaymentItem(
    val id: String,
    val customerName: String,
    val amount: Double,
    val date: String,
    val notes: String,
    val paymentType: String, // "نقدي", "فودافون كاش", "أنستا باي"
    val userId: String
)