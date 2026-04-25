package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="OutboundDetailes")
data class OutboundDetailesEntity(
    @PrimaryKey()
    val id: String = java.util.UUID.randomUUID().toString(), // توليد ID فريد عالمياً    val outbound_id: Int,
    val outboundId: String,
    val itemId: Int,
    val amount: Int,
    val price: Double,
    val isSynced: Boolean,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()


)
