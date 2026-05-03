package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.ItemMovementProjection
import com.example.myapplication.data.local.entity.StockEntity
import com.example.myapplication.data.local.entity.StockWithItemName
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stockEntity: StockEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockList(stocks: List<StockEntity>)

    // دالة مفيدة لتصفير المخزن قبل المزامنة الشاملة (اختياري)
    @Query("DELETE FROM Stock WHERE userId = :userId")
    suspend fun clearStockForUser(userId: String)
    @Query("SELECT * FROM stock")
    suspend fun getAllStockStatic(): List<StockEntity>
    @Query("SELECT * FROM Stock WHERE ItemId = :itemId LIMIT 1")
    fun getStockByItemIdFlow(itemId: Int): Flow<StockEntity?>
    @Query("""
    SELECT 
        Stock.id, 
        Stock.ItemId, 
        Stock.userId, 
        IFNULL(Items.itemName, 'صنف غير معرف') as itemName, 
        Stock.CurrentAmount, 
        Stock.InitAmount, 
        Stock.fristDate
    FROM Stock 
    LEFT JOIN Items ON TRIM(Stock.ItemId) = CAST(Items.id AS TEXT)
""")
    fun getAllStockWithNames(): Flow<List<StockWithItemName>>
    // تحديث الكمية الحالية بإضافة الكمية الواردة
    @Query("UPDATE Stock SET CurrentAmount = CurrentAmount + :addedAmount WHERE ItemId = :itemId")
    suspend fun updateStockQuantity(itemId: Int, addedAmount: Int): Int

    // للتحقق من وجود الصنف في المخزن قبل التحديث
    @Query("SELECT COUNT(*) FROM Stock WHERE ItemId = :itemId")
    suspend fun isItemInStock(itemId: Int): Int

    @Query("UPDATE Stock SET CurrentAmount = CurrentAmount - :amount WHERE itemId = :id")
    suspend fun reduceStock(id: Int, amount: Int)

    @Query("UPDATE stock SET CurrentAmount = :amount, updated_at = :currentTime WHERE ItemId = :itemId")
    suspend fun updateCurrentStock(itemId: Int, amount: Int, currentTime: Long = System.currentTimeMillis())


    @Query("UPDATE stock SET isSynced = 0, updated_at = :currentTime WHERE ItemId = :itemId")
    suspend fun markStockAsUnsynced(itemId: Int, currentTime: Long = System.currentTimeMillis())
    @Query("""
    -- 1. المشتريات (Inbound) -> تزيد المخزن (qtyIn)
    SELECT 
        i.inboundDate as date, 
        'شراء' as transactionType, 
        CAST(i.invorseNum AS TEXT) as documentNumber, 
        id.amount as qtyIn, 
        0 as qtyOut
    FROM Inbound i
    JOIN InboundDetailes id ON i.id = id.InboundId
    WHERE id.ItemId = :itemId

    UNION ALL

    -- 2. المبيعات (Outbound) -> تنقص المخزن (qtyOut)
    SELECT 
        o.outboundDate as date, 
        'بيع' as transactionType, 
        CAST(o.invorseNumber AS TEXT) as documentNumber, 
        0 as qtyIn, 
        od.amount as qtyOut
    FROM Outbound o
    JOIN OutboundDetailes od ON o.id = od.outboundId
    WHERE od.itemId = :itemId

    UNION ALL

    -- 3. المرتجعات (ReturnedDetails) -> تزيد المخزن (qtyIn)
    -- ملاحظة: افترضت وجود جدول Returned رئيسي يحتوي على التاريخ
    -- إذا كان التاريخ في نفس الجدول، عدل r.date إلى اسم الحقل لديك
    SELECT 
        'مرتجع' as date, -- يفضل ربطه بجدول المرتجع الرئيسي لجلب التاريخ
        'مرتجع' as transactionType, 
        'R-' || rd.returnedId as documentNumber, 
        rd.amount as qtyIn, 
        0 as qtyOut
    FROM returnedDetails rd
    WHERE rd.itemId = :itemId

    ORDER BY date ASC
""")
    fun getItemMovement(itemId: Int): Flow<List<ItemMovementProjection>>}