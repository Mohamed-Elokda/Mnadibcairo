package com.example.myapplication.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StockDto(
    @SerialName("id")
    val id: Int? = null,

    @SerialName("item_id")
    val itemId: Int,

    @SerialName("user_id")
    val userId: String,

    @SerialName("current_amount")
    val currentAmount: Double,

    @SerialName("updated_at")
    val updatedAt: String? = null
)