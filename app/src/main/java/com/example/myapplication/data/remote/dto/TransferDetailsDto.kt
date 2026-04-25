package com.example.myapplication.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.myapplication.data.local.entity.TransferDetailsEntity

@Serializable
data class TransferDetailsDto(
    @SerialName("id")
    val id: String,

    @SerialName("transfer_id") // الاسم كما هو في Supabase
    val transferId: String,

    @SerialName("item_id")
    val itemId: Int,

    @SerialName("amount")
    val amount: Double,

    @SerialName("created_at")
    val createdAt: String? = null
) {
    // دالة التحويل لـ Room Entity
    fun toEntity(): TransferDetailsEntity {
        return TransferDetailsEntity(

            transferId = this.transferId,
            itemId = this.itemId,
            amount = this.amount.toInt()
            // يمكنك إضافة الـ updatedAt لو محتاجه
        )
    }
}