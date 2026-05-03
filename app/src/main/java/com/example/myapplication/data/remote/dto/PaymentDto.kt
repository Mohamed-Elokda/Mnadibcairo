package com.example.myapplication.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaymentDto(
    val id: String,
    val user_id: String,
    val customer_id: Int,      // تأكد من مطابقة الاسم في السيرفر
    val amount: Double,
    val payment_type: String,  // تأكد من مطابقة الاسم في السيرفر
    val date: String,
    val notes: String ,
    val updated_at: Long       // هذا هو الحقل الذي نستخدمه للمقارنة
)