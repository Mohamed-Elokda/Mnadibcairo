package com.example.myapplication

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceItemDetail(
    val item_name: String,
    val quantity: Int,
    val unit_price: Double
)