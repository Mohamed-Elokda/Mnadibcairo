package com.example.myapplication.data.local.entity

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
    val isSynced: Boolean


)
