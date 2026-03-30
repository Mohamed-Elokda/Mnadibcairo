package com.example.myapplication.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReturnedDetailsDto(
    val id: Int? = null,
    val returned_id: Int,
    val item_id: Int,
    val amount: Int,
    val price: Double
)