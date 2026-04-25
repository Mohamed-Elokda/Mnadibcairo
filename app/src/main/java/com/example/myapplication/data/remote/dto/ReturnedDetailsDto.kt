package com.example.myapplication.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReturnedDetailsDto(
    val id: String,
    val returned_id: String,
    val item_id: Int,
    val amount: Int,
    val price: Double,
    val updated_at: Long

)