package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.myapplication.data.local.entity.ItemHistoryProjection
import com.example.myapplication.data.local.entity.ItemMovementProjection
import com.example.myapplication.data.local.entity.OutboundEntity
import com.example.myapplication.data.local.entity.OutboundWithCustomer
import com.example.myapplication.data.local.entity.OutboundWithDetails
import com.example.myapplication.domin.model.Outbound
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outbound: OutboundEntity): Long

    // في OutboundDao.kt
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutbounds(outbounds: List<OutboundEntity>)

    // في OutboundDetailesDao.kt

    @Query("SELECT * FROM Outbound where userId=:userId")
    fun getAllOutbound(userId: Int): Flow<List<OutboundEntity>>

    @Query("""
    SELECT 
        Outbound.*, 
        COALESCE(Customer.customerName, 'عميل غير معروف') as customerName 
    FROM Outbound 
    LEFT JOIN Customer ON Outbound.customerId = Customer.id 
    WHERE Outbound.userId = :userId
""")
    fun getAllOutboundWithCustomer(userId: String): Flow<List<OutboundWithCustomer>>

    @Query("SELECT * FROM Outbound WHERE isSynced = 0")
    suspend fun getUnsyncedOutbounds(): List<OutboundEntity>

    @Query("UPDATE Outbound SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)
    @Transaction // ضروري عند استخدام @Relation
    @Query("SELECT * FROM Outbound WHERE customerId = :customerId ORDER BY outboundDate ASC")
    fun getOutboundsByCustomer(customerId: Int): Flow<List<OutboundWithDetails>>

    @Query("""
        SELECT 
            o.outboundDate as date, 
            'بيع' as transactionType, 
            CAST(o.invorseNumber AS TEXT) as documentNumber, 
            c.customerName as partyName, -- جلب اسم العميل
            0 as qtyIn, 
            od.amount as qtyOut
        FROM Outbound o
        JOIN OutboundDetailes od ON o.id = od.outboundId
        LEFT JOIN Customer c ON o.customerId = c.id -- ربط جدول العملاء
        WHERE od.itemId = :itemId
    """)
    fun getOutboundByItem(itemId: Int): Flow<List<ItemMovementProjection>>

    // في OutboundDao
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

    @Query("SELECT COUNT(*) FROM outbound") // تأكد من اسم الجدول لديك
    suspend fun getItemsCount(): Int
    @Delete
    suspend fun deleteOutbound(outbound: OutboundEntity)

}


