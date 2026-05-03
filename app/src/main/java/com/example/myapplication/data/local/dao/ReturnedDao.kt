package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.myapplication.data.local.entity.ItemMovementProjection
import com.example.myapplication.data.local.entity.ReturnedDetailsEntity
import com.example.myapplication.data.local.entity.ReturnedEntity
import com.example.myapplication.data.local.entity.ReturnedWithDetails
import com.example.myapplication.data.local.entity.ReturnedWithNames
import com.example.myapplication.domin.model.CustomerTotal
import com.example.myapplication.domin.model.ItemQuantity
import kotlinx.coroutines.flow.Flow
import kotlin.contracts.Returns

@Dao
interface ReturnedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(returnedEntity: ReturnedEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnsList(returns: List<ReturnedEntity>)
    @Query("""
    SELECT 
        r.*, 
        c.customerName AS customerName,
        (SELECT itemName FROM items LIMIT 1) AS itemName 
    FROM returned r
    LEFT JOIN customer c ON r.customerId = c.id
""")
    fun getAllReturnedWithNames(): Flow<List<ReturnedWithNames>>
    @Query("""
    SELECT itemId as itemId, SUM(amount) as totalQty 
    FROM ReturnedDetails 
    GROUP BY itemId
""")
    fun getAllReturnsQtyByItem(): Flow<List<ItemQuantity>>

    @Query("""
    SELECT returned.customerId as customerId, SUM(ReturnedDetails.amount * ReturnedDetails.price) as totalAmount 
    FROM returned 
    JOIN ReturnedDetails ON returned.id = ReturnedDetails.returnedId 
    GROUP BY returned.customerId
""")
    fun getAllReturnsTotal(): Flow<List<CustomerTotal>>

    @Transaction
    @Query("SELECT * FROM returned WHERE customerId = :customerId ORDER BY returnedDate ASC")
    fun getReturnsByCustomer(customerId: Int): Flow<List<ReturnedWithDetails>>

    @Query("""
    SELECT 
        'مرتجع' as date, -- يفضل ربطه بالجدول الرئيسي لجلب التاريخ الحقيقي
        'مرتجع' as transactionType, 
        'R-' || rd.returnedId as documentNumber, 
        rd.amount as qtyIn, 
        0 as qtyOut
    FROM returnedDetails rd
    WHERE rd.itemId = :itemId
""")
    fun getReturnsByItem(itemId: Int): Flow<List<ItemMovementProjection>>

    @Query("SELECT * FROM returned")
    suspend fun getUnsyncedReturns(): List<ReturnedEntity>

    @Query("UPDATE returned SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)


    @Query("SELECT * FROM returned WHERE id = :id")
    suspend fun getReturnedByIdStatic(id: String): ReturnedEntity


    @Update
    suspend fun update(returned: ReturnedEntity)

    @Delete
    suspend fun delete(returned: ReturnedEntity)

    @Query("SELECT * FROM returnedDetails WHERE itemId = :itemId")
    suspend fun getReturnsByItemStatic(itemId: Int): List<ReturnedDetailsEntity>

    @Transaction
    @Query("SELECT * FROM returned WHERE customerId = :customerId")
    suspend fun getReturnsByCustomerStatic(customerId: Int): List<ReturnedWithDetails>
}