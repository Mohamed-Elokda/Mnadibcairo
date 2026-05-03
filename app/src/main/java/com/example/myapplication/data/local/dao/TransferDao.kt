package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.ItemMovementProjection
import com.example.myapplication.data.local.entity.TransferDetailsEntity
import com.example.myapplication.data.local.entity.TransferEntity
import com.example.myapplication.domin.model.ItemMovement
import com.example.myapplication.domin.model.ItemQuantity
import com.example.myapplication.domin.model.TransferDetailWithItemName
import com.example.myapplication.domin.model.TransferWithStoreName
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransferList(transfer: List<TransferEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransferDetail(detail: TransferDetailsEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransferDetailList(detail: List<TransferDetailsEntity>)
    @Query("""
    SELECT itemId as itemId, SUM(amount) as totalQty 
    FROM TransferDetails 
    GROUP BY itemId
""")
    fun getAllTransfersQtyByItem(): Flow<List<ItemQuantity>>
    @Query("""
    SELECT 
        t.id, 
        t.transferNum, 
        t.fromStoreId, 
        t.toStoreId, 
        s.suppliedName AS toStoreName, -- بنجيب الاسم من جدول الموردين/المخازن
        t.date, 
        t.userId, 
        t.isSynced
    FROM Transfer t
    INNER JOIN Supplied s ON t.toStoreId = s.id
    WHERE t.userId = :userId
""")
    fun getAllTransfersWithStoreName(userId: String): Flow<List<TransferWithStoreName>>
    @Query("""
        SELECT 
            o.date as date, 
            'اخراج' as transactionType, 
            CAST(o.transferNum AS TEXT) as documentNumber, 
            c.suppliedName as partyName, -- جلب اسم العميل
            0 as qtyIn, 
            od.amount as qtyOut
        FROM transfer o
        JOIN transferdetails od ON o.id = od.transferId
        LEFT JOIN supplied c ON o.toStoreId = c.id -- ربط جدول العملاء
        WHERE od.itemId = :itemId
    """)
    fun getTransferByItem(itemId: Int): Flow<List<ItemMovementProjection>>

        @Query("SELECT * FROM Transfer ")
        suspend fun getUnsyncedTransfers(): List<TransferEntity>

        @Query("SELECT * FROM TransferDetails WHERE transferId = :tId")
        suspend fun getDetailsByTransferIdSync(tId: String): List<TransferDetailsEntity>

        @Query("UPDATE Transfer SET isSynced = :status WHERE id = :id")
        suspend fun updateSyncStatus(id: String, status: Boolean)

        @Query("DELETE FROM Transfer WHERE id = :transferId")
        suspend fun deleteTransferById(transferId: String)

        // لو مش عامل Cascade، امسح التفاصيل يدويًا كمان
        @Query("DELETE FROM TransferDetails WHERE transferId = :transferId")
        suspend fun deleteTransferDetailsByParentId(transferId: String)
    @Query("""
    SELECT 
        td.id AS id, 
        td.transferId AS transferId, 
        td.itemId AS itemId, 
        i.itemName AS itemName, 
        td.amount AS quantity  -- هنا بنحول amount لـ quantity عشان الـ Model يفهمها
    FROM TransferDetails td
    INNER JOIN Items i ON td.itemId = i.id 
    WHERE td.transferId = :transferId
""")
    fun getTransferDetailsWithNames(transferId: String): Flow<List<TransferDetailWithItemName>>
    @Query("SELECT * FROM transferdetails WHERE itemId = :itemId")
    suspend fun getTransfersByItemStatic(itemId: Int): List<TransferDetailsEntity>


    // ... الدوال السابقة (insertTransfer, etc)
    }
