package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.myapplication.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments ORDER BY date DESC")
    suspend fun getAllPayments(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY date ASC")
    fun getPaymentsByCustomer(customerId: Int): Flow<List<PaymentEntity>>
}