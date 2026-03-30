package com.example.myapplication.data.local.entity

import androidx.room.Embedded

data class OutboundWithCustomer(
    @Embedded val outbound: OutboundEntity, // يجلب كل حقول الفاتورة
    val customerName: String?)