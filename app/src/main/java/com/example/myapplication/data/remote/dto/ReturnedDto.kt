package com.example.myapplication.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReturnedDto(
    val id: Int? = null,
    val customer_id: Int,
    val user_id: String,
    val returned_date: String,
    val latitude: Double,
    val longitude: Double,
    val updated_at: Long? = null // ستقوم Supabase بتعبئته تلقائياً عند الجلب

)
