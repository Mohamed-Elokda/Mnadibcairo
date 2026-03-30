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
            returnedDao.getReturnsByItem(itemId)
        ) { inbounds, outbounds, returns ->
            val allMovements = mutableListOf<ItemMovement>()

            // 1. إضافة المشتريات (Inbound)
            // نستخدم الأسماء الجديدة: it.date, it.transactionType, it.documentNumber, it.qtyIn
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

            // 2. إضافة المبيعات (Outbound)
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

            // 3. إضافة المرتجعات (Returned)
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

            // الترتيب حسب التاريخ (تصاعدياً)
            val sortedList = allMovements.sortedBy { it.date }

            // حساب الرصيد التراكمي للمخزن
            var currentStock = 0
            sortedList.forEach { movement ->
                currentStock += (movement.qtyIn - movement.qtyOut)
                movement.runningStock = currentStock
            }

            sortedList
        }


    }

}