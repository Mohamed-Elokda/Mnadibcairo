package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="OutboundDetailes")
data class OutboundDetailesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val outboundId: Long,
    val itemId: Int,
    val amount: Int,
    val price: Double,
    val isSynced: Boolean,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()


)
