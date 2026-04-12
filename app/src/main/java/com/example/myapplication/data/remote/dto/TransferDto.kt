package com.example.myapplication.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransferDto(
    val id: Int? = null,
    @SerialName("from_store_id") val fromStoreId: Int,
    @SerialName("to_store_id") val toStoreId: Int,
    @SerialName("transfer_num") val transferNum: Int,
    @SerialName("transfer_date") val date: String,
    @SerialName("user_id") val userId: String,
    val updated_at: Long? = null

)