package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.InboundDetailWithItemName
import com.example.myapplication.data.local.entity.ItemHistoryProjection
import com.example.myapplication.data.local.entity.ItemsEntity
import com.example.myapplication.data.local.entity.OutboundDetailWithItemName
import com.example.myapplication.data.local.entity.OutboundDetailesEntity
import com.example.myapplication.data.local.entity.OutboundEntity
import com.example.myapplication.data.local.entity.OutboundWithDetails
import com.example.myapplication.domin.model.OutboundDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboundDetailesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outboundDetailes: OutboundDetailesEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<OutboundDetailesEntity>)
    @Query("SELECT * FROM OutboundDetailes")
    fun getAllOutboundDetailes(): Flow<List<OutboundDetailesEntity>>


    @Query("""
    SELECT 
        OutboundDetailes.id , 
        OutboundDetailes.itemId as itemId, 
        Items.itemName as itemName, 
        OutboundDetailes.amount as quantity, 
        OutboundDetailes.price as price
    FROM OutboundDetailes 
    INNER JOIN Items ON OutboundDetailes.itemId = Items.id
    WHERE OutboundDetailes.outboundId = :outboundId
""")
    fun getDetailsListByOutboundId(outboundId: String): Flow<List<OutboundDetailWithItemName>>
    @Query("SELECT * FROM outbounddetailes WHERE outboundId = :outboundId")
    suspend fun getUnsyncedOutbounds(outboundId: String): List<OutboundDetailesEntity>

    @Query("UPDATE outbounddetailes SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)


    @Query("DELETE FROM OutboundDetailes WHERE outboundId = :outboundId")
    suspend fun deleteDetailsByOutboundId(outboundId: String)

    @Query("SELECT * FROM OutboundDetailes WHERE outboundId = :outboundId")
    suspend fun getDetailsByOutboundIdStatic(outboundId: String): List<OutboundDetailesEntity>

    @Query("""
    SELECT DISTINCT i.* FROM items i
    INNER JOIN OutboundDetailes od ON i.id = od.itemId
    INNER JOIN Outbound o ON od.outboundId = o.id
   
""")
    fun getItemsBoughtByCustomer(): Flow<List<ItemsEntity>>

    // استعلام إضافي لجلب سعر آخر عملية بيع لهذا الصنف لهذا العميل
    @Query("""
    SELECT od.price FROM OutboundDetailes od
    INNER JOIN Outbound o ON od.outboundId = o.id
    WHERE o.customerId = :customerId AND od.itemId = :itemId
    ORDER BY o.id DESC LIMIT 1
""")
    suspend fun getLastSellingPrice(customerId: Int, itemId: Int): Double?


    @Query("""
    SELECT 
        o.outboundDate as date,
        od.price as price,
        od.amount as amount
    FROM Outbound o
    JOIN OutboundDetailes od ON o.id = od.outboundId
    WHERE o.customerId = :customerId AND od.itemId = :itemId
    ORDER BY o.outboundDate DESC
""")
    fun getItemPurchaseHistory(customerId: Int, itemId: Int): Flow<List<ItemHistoryProjection>>

}


