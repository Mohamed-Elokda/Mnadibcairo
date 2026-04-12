package com.example.myapplication.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutboundDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("customer_id") val customerId: Int,
    @SerialName("invoice_number") val invoiceNumber: Int,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("outbound_date") val outboundDate: String? = null,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("money_receive") val moneyReceive: Int,
    val updated_at: Long? = null // ستقوم Supabase بتعبئته تلقائياً عند الجلب

)