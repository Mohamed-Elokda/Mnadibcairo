package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.local.dao.InboundDao
import com.example.myapplication.data.local.dao.OutboundDao
import com.example.myapplication.data.local.dao.ReturnedDao
import com.example.myapplication.data.local.dao.StockDao
import com.example.myapplication.data.local.dao.TransferDao
import com.example.myapplication.data.remote.dto.OutboundDto
import com.example.myapplication.data.remote.dto.StockDto
import com.example.myapplication.data.toDomain
import com.example.myapplication.data.toEntity
import com.example.myapplication.domin.model.ItemMovement
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.repository.StockRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StockRepoImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val inboundDao: InboundDao,
    private val outboundDao: OutboundDao,
    private val returnedDao: ReturnedDao,
    private val trDto: TransferDao,
    private val stockDao: StockDao,

) : StockRepository {

    override fun getStockData(): Flow<List<Stock>> {
        return combine(
            stockDao.getAllStockWithNames(),
            inboundDao.getAllInboundQtyByItem(), // دالة ترجع مجموع الوارد لكل صنف
            outboundDao.getAllOutboundQtyByItem(), // دالة ترجع مجموع الصادر لكل صنف
            returnedDao.getAllReturnsQtyByItem(), // دالة ترجع مجموع المرتجع لكل صنف
            trDto.getAllTransfersQtyByItem() // دالة ترجع مجموع التحويلات لكل صنف
        ) { stocks, inbounds, outbounds, returns, transfers ->

            stocks.map { entity ->
                val itemId = entity.ItemId

                // حساب الإجمالي لكل نوع حركة لهذا الصنف
                val totalIn = inbounds.filter { it.itemId == itemId }.sumOf { it.totalQty }
                val totalOut = outbounds.filter { it.itemId == itemId }.sumOf { it.totalQty }
                val totalReturns = returns.filter { it.itemId == itemId }.sumOf { it.totalQty }
                val totalTransfers = transfers.filter { it.itemId == itemId }.sumOf { it.totalQty }

                // الحسبة النهائية: (رصيد أول + وارد + مرتجع) - (صادر + تحويل صادر)
                val finalQty = (entity.InitAmount + totalIn + totalReturns) - (totalOut + totalTransfers)
                Log.d("TAG", "getStockData: "+entity.InitAmount+"-"+totalIn +"--" +totalReturns+"-"+ totalOut +""+ totalTransfers)
                entity.toDomain().copy(InitAmount = finalQty)
            }
        }
    }
    // داخل OutboundRepoImpl.kt
    override suspend fun syncStockFromServer(userId: String) {
        try {
            // جلب حالة المخزن الحالية للمندوب من السيرفر
            val remoteStock = supabase.from("stock")
                .select { filter { eq("user_id", userId) } }
                .decodeList<StockDto>()

            if (remoteStock.isNotEmpty()) {
                val entities = remoteStock.map { it.toEntity() }
                stockDao.insertStockList(entities) // تأكد من إضافة هذه الدالة في StockDao
            }
        } catch (e: Exception) {
            Log.e("Sync", "خطأ في جلب المخزن: ${e.message}")
        }
    }
    private val pendingUpdates = mutableSetOf<Int>()

    override fun getItemMovement(itemId: Int): Flow<List<ItemMovement>> {
        return combine(
            inboundDao.getInboundByItem(itemId),
            outboundDao.getOutboundByItem(itemId),
            returnedDao.getReturnsByItem(itemId),
            trDto.getTransferByItem(itemId),
            stockDao.getStockByItemIdFlow(itemId) // أضفنا تدفق بيانات المخزن هنا
        ) { inbounds, outbounds, returns, trDto,stock->
            val allMovements = mutableListOf<ItemMovement>()

            // 1. استخراج الرصيد الافتتاحي وتاريخ البدء
            val initialQty = stock?.InitAmount ?: 0
            val startDate = stock?.fristDate ?: ""

            // 2. إضافة حركات الوارد (Inbound)
            inbounds.forEach {
                allMovements.add(ItemMovement(
                    date = it.date,
                    transactionType = it.transactionType,
                    documentNumber = it.documentNumber,
                    qtyIn = it.qtyIn,
                    qtyOut = 0,
                    partyName = it.partyName,
                    runningStock = 0
                ))
            }

            // 3. إضافة حركات الصادر (Outbound)
            outbounds.forEach {
                allMovements.add(ItemMovement(
                    date = it.date,
                    transactionType = it.transactionType,
                    documentNumber = it.documentNumber,
                    qtyIn = 0,
                    qtyOut = it.qtyOut,
                    partyName = it.partyName,
                    runningStock = 0
                ))
            }

            // 4. إضافة المرتجعات
            returns.forEach {
                allMovements.add(ItemMovement(
                    date = it.date,
                    transactionType = it.transactionType,
                    documentNumber = it.documentNumber,
                    qtyIn = it.qtyIn,
                    qtyOut = 0,
                    partyName = it.partyName,
                    runningStock = 0
                ))
            }
            trDto.forEach {
                allMovements.add(ItemMovement(
                    date = it.date,
                    transactionType = it.transactionType,
                    documentNumber = it.documentNumber,
                    qtyIn = 0,
                    qtyOut =it.qtyOut,
                    partyName = it.partyName,
                    runningStock = 0
                ))
            }

            // 5. الترتيب حسب التاريخ (تصاعدياً)
            val sortedList = allMovements.sortedBy { it.date }

            // 6. بناء القائمة النهائية مع إضافة "الرصيد الافتتاحي" كأول سطر
            val finalReport = mutableListOf<ItemMovement>()

            // إضافة سطر الرصيد الافتتاحي في البداية
            var currentStock = initialQty
            finalReport.add(ItemMovement(
                date = startDate,
                transactionType = "رصيد أول المدة",
                documentNumber = "---",
                qtyIn = initialQty,
                qtyOut = 0,
                partyName = "المخزن الافتتاحي",
                runningStock = initialQty
            ))

            // 7. حساب الرصيد التراكمي بناءً على رصيد أول المدة
            sortedList.forEach { movement ->
                currentStock += (movement.qtyIn - movement.qtyOut)
                movement.runningStock = currentStock
                finalReport.add(movement)
            }




            finalReport
        }



    }

    // دالة لمزامنة كل الأصناف دفعة واحدة
    override suspend fun reconcileAllStocks() {
        withContext(Dispatchers.IO) {
            try {
                // 1. جلب كل الأصناف الموجودة في المخزن
                val allItems = stockDao.getAllStockStatic()

                allItems.forEach { stockItem ->
                    val itemId = stockItem.ItemId

                    // 2. جلب كل الحركات لهذا الصنف (Static وليس Flow)
                    val inbounds = inboundDao.getInboundsByItemStatic(itemId)
                    val outbounds = outboundDao.getOutboundsByItemStatic(itemId)
                    val returns = returnedDao.getReturnsByItemStatic(itemId)
                    val transfers = trDto.getTransfersByItemStatic(itemId)

                    // 3. الحساب المنطقي (الرصيد الافتتاحي + الوارد - الصادر)
                    var calculatedTotal = stockItem.InitAmount

                    val totalIn = inbounds.sumOf { it.amount } + returns.sumOf { it.amount }
                    val totalOut = outbounds.sumOf { it.amount } + transfers.sumOf { it.amount }

                    calculatedTotal += (totalIn - totalOut)

                    // 4. المقارنة والتحديث إذا لزم الأمر
                    if (calculatedTotal != stockItem.CurrentAmount) {
                        Log.d("Reconcile", "تم تصحيح الصنف $itemId: من ${stockItem.CurrentAmount} إلى $calculatedTotal")

                        // تحديث محلي
                        stockDao.updateCurrentStock(itemId, calculatedTotal)

                        // وسمه للمزامنة مع السيرفر
                        stockDao.markStockAsUnsynced(itemId)
                    }
                }
                Log.d("Reconcile", "✅ اكتملت عملية مطابقة كل الأصناف")
            } catch (e: Exception) {
                Log.e("Reconcile", "❌ خطأ في المزامنة الشاملة: ${e.message}")
            }
        }
    }
    private fun updateStockToMatchMovement(itemId: Int, correctAmount: Int) {
        // نستخدم CoroutineScope الخاص بالـ Repository أو الـ Global
        CoroutineScope(Dispatchers.IO).launch {
            try {
                stockDao.updateCurrentStock(itemId, correctAmount)
                // تحديث حالة المزامنة لضمان رفع الرصيد الصحيح للسيرفر
                stockDao.markStockAsUnsynced(itemId)
            } catch (e: Exception) {
                Log.e("Sync", "فشل تحديث المخزن للصنف $itemId: ${e.message}")
            }
        }
    }
}