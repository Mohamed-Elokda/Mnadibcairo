package com.example.myapplication.domin.model

data class StatementTransaction(
    val date: String,
    val description: String,
    val itemName: String = "", // اسم الصنف
    val quantity: Int = 0,      // الكمية
    val amountIn: Double,      // مديونية
    val amountOut: Double,     // سداد أو مرتجع
    var runningBalance: Double = 0.0
)