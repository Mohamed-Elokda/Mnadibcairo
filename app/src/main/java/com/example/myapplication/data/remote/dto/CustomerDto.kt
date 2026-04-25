package com.example.myapplication.data.remote.dto

import androidx.room.ColumnInfo
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class CustomerDto(
    val id: Int,
    val user_id: String,
    val customer_name: String,
    val customer_num: Int,
    val customer_debt: Double,
    val firstCustomerDebt: Double,

    val is_sync: Boolean,
    val updated_at: Long? = null // ستقوم Supabase بتعبئته تلقائياً عند الجلب

)

// دالة تحويل من Entity لـ DTO
