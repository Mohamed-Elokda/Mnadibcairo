package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TransferDetails")
data class TransferDetailsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val transferId: Int,       // ربط مع الجدول الرئيسي
    val itemId: Int,
    val amount: Int
)