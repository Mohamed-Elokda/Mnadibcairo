package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.myapplication.data.local.entity.ItemHistoryProjection
import com.example.myapplication.data.local.entity.ItemMovementProjection
import com.example.myapplication.data.local.entity.OutboundEntity
import com.example.myapplication.data.local.entity.OutboundWithCustomer
import com.example.myapplication.data.local.entity.OutboundWithDetails
import com.example.myapplication.domin.model.CustomerTotal
import com.example.myapplication.domin.model.ItemQuantity
import com.example.myapplication.domin.model.Outbound
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outbound: OutboundEntity)

    // في OutboundDao.kt
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutbounds(outbounds: List<OutboundEntity>)

    // في OutboundDetailesDao.kt

    @Query("SELECT * FROM Outbound ")
   suspend fun getAllOutbound(): List<OutboundEntity>
    @Query("""
    SELECT outbound.customerId as customerId, SUM(outbounddetailes.amount * outbounddetailes.price) as totalAmount 
    FROM outbound 
    JOIN outbounddetailes ON outbound.id = outbounddetailes.outboundId 
    GROUP BY outbound.customerId
""")
    fun getAllOutboundsTotal(): Flow<List<CustomerTotal>>
    @Query("""
    SELECT outbound.customerId as customerId, SUM(moneyResive) as totalAmount 
    FROM outbound 
    GROUP BY outbound.customerId
""")
    fun getAllOutboundsResiveTotal(): Flow<List<CustomerTotal>>
    @Query("""
    SELECT 
        Outbound.*, 
        COALESCE(Customer.customerName, 'عميل غير معروف') as customerName 
    FROM Outbound 
    LEFT JOIN Customer ON Outbound.customerId = Customer.id 
    WHERE Outbound.userId = :userId
    order by Outbound.invorseNumber
""")
    fun getAllOutboundWithCustomer(userId: String): Flow<List<OutboundWithCustomer>>

    @Query("SELECT * FROM Outbound ")
    suspend fun getUnsyncedOutbounds(): List<OutboundEntity>

    @Query("UPDATE Outbound SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
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

    @Update
    suspend fun update(outbound: OutboundEntity)

    @Query("""
    SELECT itemId as itemId, SUM(amount) as totalQty 
    FROM outbounddetailes 
    GROUP BY itemId
""")
    fun getAllOutboundQtyByItem(): Flow<List<ItemQuantity>>

}


