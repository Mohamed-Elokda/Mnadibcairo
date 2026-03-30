package com.example.myapplication.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutboundDetailsDto(
    val id: Int? = null, // أو Long حسب تعريفك
    val outbound_id: Int,
    val item_id: Int,
    val amount: Double,
    val price: Double
)