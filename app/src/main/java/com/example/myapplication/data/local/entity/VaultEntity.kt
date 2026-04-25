package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_vault")
data class VaultEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // توليد UUID تلقائياً عند الإنشاء
    val amount: Double, // المبلغ (موجب للتحصيل، سالب للتوريد)
    val operation_type: String, // "collection" أو "deposit"
    val payment_method: String, // "cash" أو "e-wallet"
    val reference_id: String? = null, // رابط بالفاتورة (اختياري)
    val notes: String? = null,
    val created_at: Long = System.currentTimeMillis(), // التوقيت الحالي
    val updated_at: Long = System.currentTimeMillis(), // وقت التعديل الأحدث
)