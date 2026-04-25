package com.example.myapplication.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransferDto(
    @SerialName("id")
    val id: String
    , // غير النوع من Int لـ String
    @SerialName("user_id")
    val userId: String,

    @SerialName("from_store_id") val fromStoreId: String,
    @SerialName("to_store_id") val toStoreId: Int,
    @SerialName("transfer_num") val transferNum: Int,
    @SerialName("transfer_date") val date: String,
    val updated_at: Long? = null

)