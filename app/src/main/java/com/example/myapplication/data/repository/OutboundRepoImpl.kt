package com.example.myapplication.data.repository

import android.util.Log
import androidx.room.Transaction
import com.example.myapplication.data.local.dao.CustomerDao
import com.example.myapplication.data.local.dao.ItemsDao
import com.example.myapplication.data.local.dao.OutboundDao
import com.example.myapplication.data.local.dao.OutboundDetailesDao
import com.example.myapplication.data.local.dao.StockDao
import com.example.myapplication.data.local.entity.OutboundDetailesEntity
import com.example.myapplication.data.local.entity.OutboundEntity
import com.example.myapplication.data.remote.dto.OutboundDetailsDto
import com.example.myapplication.data.remote.dto.OutboundDto
import com.example.myapplication.data.remote.manager.supabase
import com.example.myapplication.data.toDomain
import com.example.myapplication.data.toDto
import com.example.myapplication.data.toEntity
import com.example.myapplication.domin.model.Items
import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.model.OutboundDetails
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.repository.OutboundRepo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// OutboundRepoImpl.kt
class OutboundRepoImpl(
    private val outboundDao: OutboundDao,
    private val detailsDao: OutboundDetailesDao,
    private val stockDao: StockDao,
    private val itemsDao: ItemsDao, // تم نقله هنا
    private val customerDao: CustomerDao
) : OutboundRepo {

    @Transaction
    override suspend fun saveFullOutbound(
        outbound: Outbound,
        details: List<OutboundDetails>,
        debtAmount: Double,

    ) {
        // 1. حفظ رأس الفاتورة
        val insertedId = outboundDao.insert(outbound.toEntity())

        // 2. حفظ التفاصيل وتحديث المخزن
        details.forEach { detail ->
            val detailEntity = OutboundDetailesEntity(
                outboundId = insertedId,
                itemId = detail.itemId,
                amount = detail.amount,
                price = detail.price,
                isSynced = false
            )
            detailsDao.insert(detailEntity)

            // 3. خصم من المخزن
            stockDao.reduceStock(detail.itemId, detail.amount)
        }

        // 4. تحديث مديونية العميل (إضافة المتبقي على المديونية الحالية)
        customerDao.updateCustomerDebt(outbound.customerId, debtAmount)
    }
    override fun getAllOutbounds(userId: String): Flow<List<Outbound>> {
        return outboundDao.getAllOutboundWithCustomer(userId).map { entities ->
            entities.map { it.toDomain() } // تحتاج لدالة Mapper لتحويل Entity إلى Model
        }
    }
    override suspend fun checkItemExists(itemId: Int): Boolean {
        return itemsDao.getItemById(itemId) != null
    }



    override fun getAllItems(): Flow<List<Stock>> {
        return stockDao.getAllStockWithNames().map { entities ->
            entities.map { entity ->
                Stock(
                    id = entity.id,
                    itemName = entity.itemName,
                    ItemId = entity.ItemId,
                    userId = entity.userId,
                    InitAmount = entity.CurrentAmount,
                    CurrentAmount = entity.CurrentAmount,
                    fristDate = entity.fristDate,
                    isSynced = true
                )
            }
        }
    }

   override suspend fun deleteInvoiceAndRollbackAll(outbound: Outbound) {
        // 1. جلب تفاصيل الأصناف لإعادتها للمخزن
        val details = detailsDao.getDetailsByOutboundIdStatic(outbound.id.toLong())

        // 2. حساب إجمالي الفاتورة الفعلي من الأصناف (أو استخدم مبلغه المسجل)
        val totalInvoicePrice = details.sumOf { it.amount * it.price }-outbound.moneyResive

        // 3. تنفيذ العمليات داخل قاعدة البيانات

        // أ- إعادة الكميات للمخزن
        details.forEach { detail ->
            stockDao.updateStockQuantity(detail.itemId, detail.amount)
        }

        // ب- تعديل حساب العميل (إرجاع الرصيد لما كان عليه)
        // نخصم إجمالي الفاتورة من مديونية العميل لأن الفاتورة لُغيت
        customerDao.decreaseCustomerBalance(outbound.customerId, totalInvoicePrice)

        // ج- حذف التفاصيل والرأس
        detailsDao.deleteDetailsByOutboundId(outbound.id.toLong())
        outboundDao.deleteOutbound(outbound.toEntity())
    }
    // داخل OutboundRepoImpl
    suspend fun syncEverything() {
        try {
            // 1. مزامنة العملاء أولاً
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()
            unsyncedCustomers.forEach { customer ->
                try {
                    supabase.from("customers").upsert(customer.toDto())
                    customerDao.markAsSynced(customer.id)
                } catch (e: Exception) {
                    Log.e("cxcxcxcxcxc", "Failed to sync customer ${customer.id}: ${e.message}")
                    return@forEach
                }
            }

            // 2. مزامنة الفواتير وتفاصيلها
            val unsyncedOutbounds = outboundDao.getUnsyncedOutbounds()
            unsyncedOutbounds.forEach { outbound ->
                try {
                    // أ: رفع رأس الفاتورة
                    val outboundDto = outbound.toDto()
                    supabase.from("outbound").upsert(outboundDto)

                    // ب: جلب تفاصيل هذه الفاتورة تحديداً من قاعدة البيانات المحلية
                    // تأكد أن لديك دالة في Dao تجلب التفاصيل بـ outbound_id
                    val details = detailsDao.getUnsyncedOutbounds(outbound.id)

                    if (details.isNotEmpty()) {
                        val detailsDtos = details.map { it.toDto() }

                        // ج: رفع كل التفاصيل دفعة واحدة (Bulk Insert) للسيرفر
                        supabase.from("outbound_details").upsert(detailsDtos)
                    }

                    // د: تحديث حالة الفاتورة محلياً بعد نجاح رفعها هي وتفاصيلها
                    outboundDao.markAsSynced(outbound.id)
                    // (اختياري) تحديث حالة التفاصيل أيضاً إذا كنت تتبع حالتها
                    detailsDao.markAsSynced(outbound.id)

                    Log.d("cxcxcxcxcxc", "Invoice ${outbound.invorseNumber} and its details synced")

                } catch (e: Exception) {
                    Log.e("cxcxcxcxcxc", "Failed to sync outbound ${outbound.id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("cxcxcxcxcxc", "General Sync Error: ${e.message}")
        }
    }
    override suspend fun getItemsCount(): Int {
        return outboundDao.getItemsCount()
    }
    override suspend fun syncOutboundsFromServer(userId: String) {
        try {
            // 1. جلب رؤوس الفواتير الخاصة بهذا المستخدم من Supabase
            val remoteOutbounds = supabase.from("outbound")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }.decodeList<OutboundDto>() // تأكد أن موديل Outbound متوافق مع DTO

            if (remoteOutbounds.isNotEmpty()) {
                // تحويل إلى Entity وحفظ في Room
                val outboundEntities = remoteOutbounds.map {
                   it .toEntity()
                }
                outboundDao.insertOutbounds(outboundEntities)

                // 2. جلب كافة تفاصيل هذه الفواتير
                // سنقوم بجلب التفاصيل لكل فاتورة تم تحميلها
                remoteOutbounds.forEach { outbound ->
                    val remoteDetails = supabase.from("outbound_details")
                        .select {
                            filter {
                                eq("outbound_id", outbound.id?:0)
                            }
                        }.decodeList<OutboundDetailsDto>()

                    if (remoteDetails.isNotEmpty()) {
                        val detailEntities = remoteDetails.map { it.toEntity(outbound.id?.toLong()?:0)}
                        detailsDao.insertDetails(detailEntities)
                    }
                }
                Log.d("SyncCheck", "Downloaded ${remoteOutbounds.size} invoices and their details")
            }
        } catch (e: Exception) {
            Log.e("SyncCheck", "Failed to download outbounds: ${e.message}")
        }
    }
    override suspend fun syncItemsFromServer() {
        try {
            val remoteItems = supabase.from("items").select().decodeList<Items>()
            Log.d("SyncCheck", "Fetched from Supabase: ${remoteItems.size} items") // أضف هذا
            val entities = remoteItems.map { it.toEntity() }
            itemsDao.insertItemsList(entities)
            Log.d("SyncCheck", "Saved to Room successfully") // أضف هذا
        } catch (e: Exception) {
            Log.e("SyncCheck", "Error: ${e.message}")
        }
    }
}


