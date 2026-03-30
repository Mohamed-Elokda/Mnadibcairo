package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.myapplication.data.local.entity.ItemMovementProjection
import com.example.myapplication.data.local.entity.ReturnedEntity
import com.example.myapplication.data.local.entity.ReturnedWithDetails
import com.example.myapplication.data.local.entity.ReturnedWithNames
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

    @Query("SELECT * FROM returned WHERE isSynced = 0")
    suspend fun getUnsyncedReturns(): List<ReturnedEntity>

    @Query("UPDATE returned SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)
}