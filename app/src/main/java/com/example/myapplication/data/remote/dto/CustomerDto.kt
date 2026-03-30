package com.example.myapplication.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class CustomerDto(
    val id: Int,
    val user_id: String,
    val customer_name: String,
    val customer_num: Int,
    val customer_debt: Double,

    val is_sync: Boolean
)

// دالة تحويل من Entity لـ DTO
