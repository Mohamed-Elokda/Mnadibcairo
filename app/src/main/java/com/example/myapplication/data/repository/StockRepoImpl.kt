package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.local.dao.InboundDao
import com.example.myapplication.data.local.dao.OutboundDao
import com.example.myapplication.data.local.dao.ReturnedDao
import com.example.myapplication.data.local.dao.StockDao
import com.example.myapplication.data.remote.dto.StockDto
import com.example.myapplication.data.remote.manager.supabase
import com.example.myapplication.data.toDomain
import com.example.myapplication.data.toEntity
import com.example.myapplication.domin.model.ItemMovement
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.repository.StockRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class StockRepoImpl(
    private val inboundDao: InboundDao,
    private val outboundDao: OutboundDao,
    private val returnedDao: ReturnedDao,
    private val stockDao: StockDao,

) : StockRepository {

    override fun getStockData(): Flow<List<Stock>> {
        return stockDao.getAllStockWithNames().map { list ->
            list.map { it.toDomain() } // تأكد من وجود دالة Mapper تحول StockEntity لـ Stock
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

    override fun getItemMovement(itemId: Int): Flow<List<ItemMovement>> {
        return combine(
            inboundDao.getInboundByItem(itemId),
            outboundDao.getOutboundByItem(itemId),
            returnedDao.getReturnsByItem(itemId),
            stockDao.getStockByItemIdFlow(itemId) // أضفنا تدفق بيانات المخزن هنا
        ) { inbounds, outbounds, returns, stock ->
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
}