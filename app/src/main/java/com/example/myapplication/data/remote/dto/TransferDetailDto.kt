package com.example.myapplication.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransferDetailDto(
    @SerialName("transfer_id") val transferId: Int,
    @SerialName("item_id") val itemId: Int,
    val amount: Int
)