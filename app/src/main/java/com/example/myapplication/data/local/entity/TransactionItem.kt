package com.example.myapplication.data.local.entity

data class TransactionItem(
    val date: String,
    val description: String,
    val amountIn: Double,  // مبيعات (تزيد الدين)
    val amountOut: Double, // تحصيل أو مرتجع (تنقص الدين)
    var runningBalance: Double = 0.0 // الرصيد بعد هذه العملية
)