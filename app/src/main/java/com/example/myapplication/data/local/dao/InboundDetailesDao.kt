package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.InboundDetailWithItemName
import com.example.myapplication.data.local.entity.InboundDetailesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InboundDetailesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inboundDetailesEntity: InboundDetailesEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInboundDetailsList(details: List<InboundDetailesEntity>)
    @Query("SELECT * FROM InboundDetailes")
    fun getAllInboundDetailes(): Flow<List<InboundDetailesEntity>>

    @Query("SELECT * FROM InboundDetailes WHERE isSynced = 0")
    suspend fun getUnsyncedDetails(): List<InboundDetailesEntity>
    @Query("""
        SELECT 
            InboundDetailes.ItemId as itemId, 
            IFNULL(Items.itemName, 'صنف غير معرف') as itemName, 
            InboundDetailes.amount as quantity, 
            0.0 as price -- أضفت 0.0 لأن حقل السعر غير موجود في Entity التفاصيل عندك حالياً
        FROM InboundDetailes 
        LEFT JOIN Items ON InboundDetailes.ItemId = Items.id
        WHERE InboundDetailes.InboundId = :inboundId
    """)
    fun getDetailsByInboundId(inboundId: Long): Flow<List<InboundDetailWithItemName>>

    @Query("SELECT * FROM InboundDetailes WHERE InboundId = :inboundId")
    suspend fun getDetailsByInboundIdSync(inboundId: Int): List<InboundDetailesEntity>

    @Query("DELETE FROM InboundDetailes WHERE InboundId = :inboundId")
    suspend fun deleteDetailsByInboundId(inboundId: Long)}