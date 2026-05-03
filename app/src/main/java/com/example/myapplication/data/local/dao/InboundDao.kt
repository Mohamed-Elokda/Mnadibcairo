package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.InboundDetailesEntity
import com.example.myapplication.data.local.entity.InboundEntity
import com.example.myapplication.data.local.entity.InboundWithSupplier
import com.example.myapplication.data.local.entity.ItemMovementProjection
import com.example.myapplication.domin.model.ItemQuantity
import kotlinx.coroutines.flow.Flow

@Dao
interface InboundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inbound: InboundEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInboundList(inbounds: List<InboundEntity>)

    @Query("SELECT * FROM inbound ")
    suspend fun getAllOutbound(): List<InboundEntity>

    @Query("UPDATE Inbound SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("""
    SELECT 
        Inbound.id, 
        Inbound.invorseNum, 
        Inbound.userId, 
        Inbound.inboundDate, 
        Inbound.fromSppliedId, 
        Inbound.latitude, 
        Inbound.longitude, 
        Inbound.image, 
        Inbound.isSynced, 
        Supplied.suppliedName AS suppliedName
    FROM Inbound
    INNER JOIN Supplied ON Inbound.fromSppliedId = Supplied.id
    WHERE Inbound.userId = :userId
""")
    fun getAllInboundWithSupplierName(userId: String): Flow<List<InboundWithSupplier>>

    @Query("SELECT * FROM Inbound")
    suspend fun getUnsyncedInbounds(): List<InboundEntity>
    
    @Query("""
        SELECT 
            i.inboundDate as date, 
            'شراء' as transactionType, 
            CAST(i.invorseNum AS TEXT) as documentNumber, 
            s.suppliedName as partyName, 
            id.amount as qtyIn, 
            0 as qtyOut
        FROM Inbound i
        JOIN InboundDetailes id ON i.id = id.InboundId
        LEFT JOIN supplied s ON i.fromSppliedId = s.id
        WHERE id.ItemId = :itemId
    """)
    fun getInboundByItem(itemId: Int): Flow<List<ItemMovementProjection>>

    @Delete
    suspend fun delete(inbound: InboundEntity)
    
    @Query("""
        SELECT ItemId as itemId, SUM(amount) as totalQty 
        FROM inbounddetailes 
        GROUP BY ItemId
    """)
    fun getAllInboundQtyByItem(): Flow<List<ItemQuantity>>

    @Query("SELECT SUM(amount) FROM inbounddetailes WHERE ItemId = :itemId")
    fun getTotalInboundByItem(itemId: Int): Flow<Int?>

    @Query("SELECT * FROM Inbound WHERE id = :id LIMIT 1")
    fun getInboundByIdSync(id: String): InboundEntity?

    @Query("SELECT * FROM inbounddetailes WHERE itemId = :itemId")
    suspend fun getInboundsByItemStatic(itemId: Int): List<InboundDetailesEntity>
}