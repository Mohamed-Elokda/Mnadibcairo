package com.example.myapplication.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransferDetailDto(
    val id: String,
    @SerialName("transfer_id") val transferId: String,
    @SerialName("item_id") val itemId: Int,
    val amount: Int,
    val updated_at: Long? = null // ستقوم Supabase بتعبئته تلقائياً عند الجلب

)