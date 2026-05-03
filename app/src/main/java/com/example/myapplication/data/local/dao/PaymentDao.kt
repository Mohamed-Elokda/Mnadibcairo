package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.PaymentEntity
import com.example.myapplication.domin.model.CustomerTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("SELECT * FROM payments ORDER BY date DESC")
    suspend fun getAllPayments(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY date ASC")
    fun getPaymentsByCustomer(customerId: Int): Flow<List<PaymentEntity>>

    @Query("SELECT customerId as customerId, SUM(amount) as totalAmount FROM Payments GROUP BY customerId")
    fun getAllPaymentsTotal(): Flow<List<CustomerTotal>>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentByIdSync(id: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE customerId = :customerId")
    suspend fun getPaymentsByCustomerStatic(customerId: Int): List<PaymentEntity>

    @Query("UPDATE payments SET updated_at = :time WHERE id = :id")
    suspend fun markAsSynced(id: String, time: Long = System.currentTimeMillis())
}