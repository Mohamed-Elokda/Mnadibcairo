package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Stock")
data class StockEntity(
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = true) // تأكد من استخدام autoGenerate إذا كنت تريد ترقيم تلقائي
    val id: Int = 0,
    val ItemId: Int = 0,
    val userId: String,
    val InitAmount: Int = 0,
    val CurrentAmount: Int = 0,
    val fristDate: String = "",
    val isSynced: Boolean,

    // العمود الجديد لتاريخ التحديث
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)