package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("InboundDetailes")
data class InboundDetailesEntity (
    @PrimaryKey()
    val id: String=java.util.UUID.randomUUID().toString(),
    val InboundId: String,
    val ItemId: Int,
    val amount:Int,
    val isSynced: Boolean=false,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)