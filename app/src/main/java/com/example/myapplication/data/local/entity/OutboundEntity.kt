package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName

@Entity(tableName = "Outbound")
data class OutboundEntity (
    @PrimaryKey()
    val id: String = java.util.UUID.randomUUID().toString(), // توليد ID فريد عالمياً    val outbound_id: Int,
    val customerId: Int,
    val invorseNumber: Int,
    val image: String,
    val outboundDate: String,
    val userId: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val moneyResive: Int, // المبلغ المدفوع حالياً

    // الأعمدة الجديدة
    val previousDebt: Double,   // الرصيد السابق (مديونية العميل قبل هذه الفاتورة)
    val totalRemainder: Double, // المتبقي النهائي (الرصيد السابق + إجمالي الفاتورة - المدفوع)

    val isSynced: Boolean,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)