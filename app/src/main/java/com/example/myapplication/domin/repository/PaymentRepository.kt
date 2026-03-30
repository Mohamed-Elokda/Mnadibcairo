package com.example.myapplication.domin.repository

import com.example.myapplication.domin.model.PaymentItem

interface PaymentRepository {
    suspend fun processPaymentAndUpdateBalance(
        customerId: Int,
        amount: Double,
        type: String
    ): Boolean

    suspend fun getAllPayments(): List<PaymentItem>
}