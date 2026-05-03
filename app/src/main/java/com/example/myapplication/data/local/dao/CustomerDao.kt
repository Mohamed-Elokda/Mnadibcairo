package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

import com.example.myapplication.data.local.entity.Customer

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<Customer>)


    @Query("SELECT * FROM Customer WHERE userId = :userId")
    fun getAllCustomers(userId: String): Flow<List<Customer>>

    @Query("SELECT * FROM Customer WHERE isSync = 0")
    suspend fun getUnsyncedCustomers(): List<Customer>

    @Query("SELECT * FROM Customer WHERE id = :customerId")
    fun getCustomerById(customerId: Int): Flow<Customer?>

    @Query("""
    SELECT date, description, amountIn, amountOut FROM (
        -- 1. المبيعات (Outbound)
        SELECT outboundDate AS date, 
               'فاتورة مبيعات #' || invorseNumber AS description, 
               (SELECT SUM(amount * price) FROM OutboundDetailes WHERE outboundId = o.id) AS amountIn, 
               0.0 AS amountOut 
        FROM Outbound o WHERE customerId = :customerId
        
        UNION ALL
        
        -- 2. التحصيلات النقدي (Payments)
        SELECT date AS date, 
               'تحصيل نقدي (' || paymentType || ')' AS description, 
               0.0 AS amountIn, 
               amount AS amountOut 
        FROM payments WHERE customerId = :customerId
        
        UNION ALL
        
        -- 3. المرتجعات (Returned)
        SELECT returnedDate AS date, 
               'مرتجع أصناف' AS description, 
               0.0 AS amountIn, 
               (SELECT SUM(amount * price) FROM returnedDetails WHERE returnedId = r.id) AS amountOut 
        FROM returned r WHERE customerId = :customerId
    ) ORDER BY date ASC
""")
    fun getCustomerLedger(customerId: Int): Flow<List<RawTransaction>>

    // كلاس مساعد لاستقبال البيانات الخام من SQL
    data class RawTransaction(
        val date: String,
        val description: String,
        val amountIn: Double,
        val amountOut: Double
    )
    @Query("UPDATE customer SET isSync = 1 WHERE id = :customerId")
    suspend fun markAsSynced(customerId: Int)
    // في CustomerDao.kt
    @Query("UPDATE customer SET customerDebt = customerDebt - :amount WHERE id = :customerId")
    suspend fun decreaseCustomerBalance(customerId: Int, amount: Double)

    @Query("SELECT customerName FROM customer WHERE id = :customerId LIMIT 1")
    suspend fun getCustomerNameById(customerId: Int): String

// ملاحظة: إذا كنت تزيد المديونية عند البيع، فالحذف يعني "طرح" هذا المبلغ من مديونيته.

    @Query("SELECT customerName FROM Customer")
    suspend fun getAllCustomerNames(): List<String>


    @Query("SELECT * FROM customer")
    suspend fun getAllCustomersStatic(): List<Customer>

    @Query("UPDATE customer SET customerDebt = :debt WHERE id = :customerId")
    suspend fun updateCustomerDebt(customerId: Int, debt: Double)

    @Query("UPDATE customer SET  updatedAt = :time WHERE id = :customerId")
    suspend fun markCustomerAsUnsynced(customerId: Int, time: Long = System.currentTimeMillis())
}