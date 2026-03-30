package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Transfer")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val transferNum: Int,      // رقم عملية المناقلة
    val fromStoreId: Int,      // المخزن الذي خرجت منه المواد
    val toStoreId: Int,        // المخزن الذي استلم المواد
    val userId: String,
    val date: String,
    val isSynced: Boolean = false
)