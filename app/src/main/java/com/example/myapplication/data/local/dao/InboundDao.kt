package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.InboundEntity
import com.example.myapplication.data.local.entity.ItemMovementProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface InboundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inbound: InboundEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInboundList(inbounds: List<InboundEntity>)

    @Query("SELECT * FROM Inbound where userId=:userId")
    fun getAllInbound(userId: String): Flow<List<InboundEntity>>

    @Query("SELECT * FROM Inbound WHERE isSynced = 0")
    suspend fun getUnsyncedInbounds(): List<InboundEntity>

    @Query("""
        SELECT 
            i.inboundDate as date, 
            'شراء' as transactionType, 
            CAST(i.invorseNum AS TEXT) as documentNumber, 
            s.suppliedName as partyName, -- جلب اسم المورد
            id.amount as qtyIn, 
            0 as qtyOut
        FROM Inbound i
        JOIN InboundDetailes id ON i.id = id.InboundId
        LEFT JOIN supplied s ON i.fromSppliedId = s.id -- ربط جدول الموردين
        WHERE id.ItemId = :itemId
    """)
    fun getInboundByItem(itemId: Int): Flow<List<ItemMovementProjection>>

    @Delete
    suspend fun delete(inbound: InboundEntity)
}