package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Transfer")
data class TransferEntity(
    @PrimaryKey()
    val id: String = java.util.UUID.randomUUID().toString(),
    val transferNum: Int,      // رقم عملية المناقلة
    val fromStoreId: String,      // المخزن الذي خرجت منه المواد
    val toStoreId: Int,        // المخزن الذي استلم المواد
    val userId: String,
    val date: String,
    val isSynced: Boolean = false,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)