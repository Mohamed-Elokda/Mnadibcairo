package com.example.myapplication.data.remote.dto

import androidx.room.ColumnInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutboundDetailsDto(
    @SerialName("id") val id: String,
  val item_id: Int,
  val outbound_id: String,
    val amount: Double,
    val price: Double,

    val updated_at: Long? = null // ستقوم Supabase بتعبئته تلقائياً عند الجلب

)