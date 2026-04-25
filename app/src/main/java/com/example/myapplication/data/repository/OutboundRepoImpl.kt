package com.example.myapplication.data.repository

import android.content.Context
import android.util.Log
import androidx.room.Transaction
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.local.dao.CustomerDao
import com.example.myapplication.data.local.dao.InboundDao
import com.example.myapplication.data.local.dao.InboundDetailesDao
import com.example.myapplication.data.local.dao.ItemsDao
import com.example.myapplication.data.local.dao.OutboundDao
import com.example.myapplication.data.local.dao.OutboundDetailesDao
import com.example.myapplication.data.local.dao.ReturnedDao
import com.example.myapplication.data.local.dao.ReturnedDetailsDao
import com.example.myapplication.data.local.dao.StockDao
import com.example.myapplication.data.local.dao.TransferDao
import com.example.myapplication.data.local.entity.Customer
import com.example.myapplication.data.local.entity.InboundEntity
import com.example.myapplication.data.local.entity.OutboundDetailWithItemName
import com.example.myapplication.data.local.entity.OutboundDetailesEntity
import com.example.myapplication.data.local.entity.OutboundEntity
import com.example.myapplication.data.local.entity.OutboundWithDetails
import com.example.myapplication.data.local.entity.ReturnedEntity
import com.example.myapplication.data.local.entity.TransferEntity
import com.example.myapplication.data.remote.dto.CustomerDto
import com.example.myapplication.data.remote.dto.InboundDetailsDto
import com.example.myapplication.data.remote.dto.InboundDto
import com.example.myapplication.data.remote.dto.OutboundDetailsDto
import com.example.myapplication.data.remote.dto.OutboundDto
import com.example.myapplication.data.remote.dto.ReturnedDetailsDto
import com.example.myapplication.data.remote.dto.ReturnedDto
import com.example.myapplication.data.remote.dto.TransferDetailsDto
import com.example.myapplication.data.remote.dto.TransferDto
import com.example.myapplication.data.toDomain
import com.example.myapplication.data.toDto
import com.example.myapplication.data.toEntity
import com.example.myapplication.domin.model.Items
import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.model.OutboundDetails
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.repository.OutboundRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// OutboundRepoImpl.kt
class OutboundRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context, // أضف هذا
    private val supabase: SupabaseClient,
    private val outboundDao: OutboundDao,
    private val detailsDao: OutboundDetailesDao,
    private val inboundDetailesDao: InboundDetailesDao,
    private val returnedDao: ReturnedDao,
    private val returnedDetailesDao: ReturnedDetailsDao,
    private val transferDetailsDao: TransferDao,
    private val stockDao: StockDao,
    private val inboundDao: InboundDao,
    private val itemsDao: ItemsDao, // تم نقله هنا
    private val customerDao: CustomerDao
) : OutboundRepo {
    @Transaction
    override suspend fun saveFullOutbound(
        outbound: Outbound,
        details: List<OutboundDetails>,
        debtAmount: Double,
    ) {
        // 1. توليد UUID فريد للفاتورة إذا كان فارغاً
        // هذا يضمن أن الفاتورة وتفاصيلها مرتبطان بنفس المعرف من البداية
        val finalOutboundId = if (outbound.id.isNullOrEmpty()) {
            java.util.UUID.randomUUID().toString()
        } else {
            outbound.id
        }

        // 2. تحديث الكائن بالـ ID الجديد قبل الحفظ
        val outboundEntity = outbound.toEntity().copy(id = finalOutboundId)
        outboundDao.insert(outboundEntity)

        // 3. حفظ التفاصيل باستخدام الـ finalOutboundId
        details.forEach { detail ->
            val detailEntity = OutboundDetailesEntity(
                id = java.util.UUID.randomUUID().toString(), // كل تفصيلة لها ID خاص بها أيضاً
                outboundId = finalOutboundId, // الربط مع الفاتورة الأم
                itemId = detail.itemId,
                amount = detail.amount,
                price = detail.price,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )

            Log.d("SyncDebug", "Saving Detail for Outbound ID: $finalOutboundId")
            detailsDao.insert(detailEntity)

            // 4. خصم من المخزن
            stockDao.reduceStock(detail.itemId, detail.amount)
        }

        // 5. تحديث مديونية العميل
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
        try {
            val details = detailsDao.getDetailsByOutboundIdStatic(outbound.id)

            val totalInvoicePrice = details.sumOf { it.amount * it.price } - outbound.moneyResive


            try {
                // حذف التفاصيل من السيرفر
                supabase.from("outbound_details").delete {
                    filter {
                        eq("outbound_id", outbound.id)
                    }
                }

                // حذف رأس الفاتورة من السيرفر
                supabase.from("outbound").delete {
                    filter {
                        eq("id", outbound.id)
                    }
                }
                Log.d("SyncCheck", "Deleted from Supabase successfully")
            } catch (e: Exception) {
                // ملحوظة: لو فشل حذف السيرفر (بسبب النت مثلاً) ممكن تختار تكمل حذف محلي
                // أو توقف العملية، الأفضل هنا نسجل الخطأ
                Log.e("SyncCheck", "Failed to delete from remote: ${e.message}")
                FileLogger.logError( "خطأ ", e)
            }

            // 3. العمليات المحلية (Local Rollback)

            // أ- إعادة الكميات للمخزن
            details.forEach { detail ->
                stockDao.updateStockQuantity(detail.itemId, detail.amount)
            }

            // ب- تعديل حساب العميل
            customerDao.decreaseCustomerBalance(outbound.customerId, totalInvoicePrice)

            // ج- حذف التفاصيل والرأس محلياً من Room
            detailsDao.deleteDetailsByOutboundId(outbound.id)
            outboundDao.deleteOutbound(outbound.toEntity().copy(id = outbound.id))

        } catch (e: Exception) {
            Log.e("SyncCheck", "General error during delete: ${e.message}")
            FileLogger.logError("خطأ ة", e)
            throw e // لإعلام ال ViewModel بحدوث خطأ
        }
    }    // داخل OutboundRepoImpl
    override suspend fun syncEverything() {
        try {
            val currentTime = System.currentTimeMillis()

            // 1. مزامنة العملاء
            customerDao.getUnsyncedCustomers().forEach { local ->
                handleCustomerSync(local, currentTime)
            }

            // 2. مزامنة فواتير الصرف (Outbound)
            outboundDao.getAllOutbound().forEach { local ->
                handleOutboundSync(local, currentTime)
            }

            // 3. مزامنة فواتير الوارد (Inbound)
            inboundDao.getAllOutbound().forEach { local ->
                handleInboundSync(local, currentTime)
            }

            returnedDao.getUnsyncedReturns().forEach { local ->
                handleReturnedSync(local, currentTime)
            }

            transferDetailsDao.getUnsyncedTransfers().forEach { local ->
                handleTransferSync(local, currentTime)
            }

            Log.d("Sync", "✅ اكتملت المزامنة في الاتجاهين بنجاح")
        } catch (e: Exception) {
            Log.e("Sync", "خطأ عام: ${e.message}")
        }
    }

    private suspend fun handleCustomerSync(local: Customer, currentTime: Long) {
        try {
            // 1. محاولة جلب العميل من السيرفر للمقارنة
            val remote = supabase.from("customers").select {
                filter { eq("id", local.id) }
            }.decodeSingleOrNull<CustomerDto>()

            val remoteTime = remote?.updated_at ?: 0L

            when {
                // الحالة 1: الموبايل أحدث أو العميل جديد تماماً -> ارفع للسيرفر
                remote == null || local.updatedAt!! > remoteTime -> {
                    supabase.from("customers").upsert(
                        local.toDto().copy(updated_at = currentTime)
                    )
                    customerDao.markAsSynced(local.id)
                }

                // الحالة 2: السيرفر أحدث (مثلاً المدير عدل بيانات العميل أو مديونيته) -> اجلب للموبايل
                remoteTime > local.updatedAt -> {
                    // تحديث بيانات العميل محلياً في Room
                    val updatedLocal = remote.toEntity()
                    customerDao.insertCustomer(updatedLocal) // سيعمل كـ Update لأن الـ ID موجود
                    Log.d("Sync", "تم تحديث بيانات العميل ${local.customerName} من السيرفر")
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "خطأ في مزامنة العميل ${local.id}: ${e.message}")
        }
    }

    // --- دالة مزامنة الصرف المطورة ---
    private suspend fun handleOutboundSync(local: OutboundEntity, currentTime: Long) {
        try {
            val remote = supabase.from("outbound").select {
                filter { eq("id", local.id) }
            }.decodeSingleOrNull<OutboundDto>()

            val remoteTime = remote?.updated_at ?: 0L

            when {
                // الحالة 1: الموبايل أحدث أو غير موجود بالسيرفر -> Push
                remote == null || local.updatedAt > remoteTime -> {
                    supabase.from("outbound").upsert(local.toDto().copy(updated_at = currentTime))
                    supabase.from("outbound_details").delete { filter { eq("outbound_id", local.id) } }
                    val details = detailsDao.getDetailsByOutboundIdStatic(local.id)
                    if (details.isNotEmpty()) {
                        supabase.from("outbound_details").insert(details.map { it.toDto() })
                    }
                    outboundDao.markAsSynced(local.id)
                }

                // الحالة 2: السيرفر أحدث -> Pull 💡
                remoteTime > local.updatedAt -> {
                    // تحديث رأس الفاتورة محلياً
                    outboundDao.insert(remote.toEntity().copy(isSynced = true))

                    // جلب الأصناف الجديدة من السيرفر
                    val remoteDetails = supabase.from("outbound_details")
                        .select { filter { eq("outbound_id", local.id) } }
                        .decodeList<OutboundDetailsDto>()

                    // مسح الأصناف القديمة محلياً وإضافة الجديدة
                    detailsDao.deleteDetailsByOutboundId(local.id)
                    detailsDao.insertDetails(remoteDetails.map { it.toEntity(local.id).copy(isSynced = true) })
                }
            }
        } catch (e: Exception) { Log.e("Sync", "Error Outbound ${local.id}: ${e.message}") }
    }

    // --- دالة مزامنة الوارد المطورة ---
    private suspend fun handleInboundSync(local: InboundEntity, currentTime: Long) {
        try {
            val remote = supabase.from("inbound").select {
                filter { eq("id", local.id) }
            }.decodeSingleOrNull<InboundDto>()

            val remoteTime = remote?.updated_at ?: 0L

            when {
                // الحالة 1: الموبايل أحدث -> Push
                remote == null || local.updatedAt > remoteTime -> {
                    supabase.from("inbound").upsert(local.toDto().copy(updated_at = currentTime))
                    supabase.from("inbound_details").delete { filter { eq("inbound_id", local.id) } }
                    val details = inboundDetailesDao.getDetailsByOutboundIdStatic(local.id)
                    if (details.isNotEmpty()) {
                        supabase.from("inbound_details").insert(details.map { it.toDto().copy(updated_at = currentTime) })
                    }
                }


                // الحالة 2: السيرفر أحدث -> Pull 💡
                remoteTime > local.updatedAt -> {
                    inboundDao.insert(remote.toEntity().copy(isSynced = true, updatedAt = remoteTime
                    ))

                    val remoteDetails = supabase.from("inbound_details")
                        .select { filter { eq("inbound_id", local.id) } }
                        .decodeList<InboundDetailsDto>()

                    // تحديث محلي للأصناف
                    inboundDetailesDao.deleteDetailsByInboundId(local.id)
                    inboundDetailesDao.insertInboundDetailsList(remoteDetails.map { it.toEntity().copy(isSynced = true,  updatedAt = remoteTime
                    ) })
                }
            }
        } catch (e: Exception) { Log.e("Sync", "Error Inbound ${local.id}: ${e.message}") }
    }


    private suspend fun handleReturnedSync(local: ReturnedEntity, currentTime: Long) {
        try {
            val remote = supabase.from("returned").select {
                filter { eq("id", local.id) }
            }.decodeSingleOrNull<ReturnedDto>()

            val remoteTime = remote?.updated_at ?: 0L

            when {
                // الحالة 1: الموبايل أحدث أو غير موجود -> ارفع للسيرفر (Push)
                remote == null || local.updatedAt > remoteTime -> {
                    // حذف السجل القديم من السيرفر تماماً إذا كان موجوداً
                    // هذا ينهي مشكلة الـ duplicate key للأبد
                    supabase.from("returned").delete {
                        filter { eq("id", local.id) }
                    }

                    // إضافته كأنه سجل جديد
                    supabase.from("returned").insert(local.toDto().copy(updated_at = currentTime))

                    // تكملة رفع التفاصيل...
                    supabase.from("returned_details").delete { filter { eq("returned_id", local.id) } }
                    val details = returnedDetailesDao.getDetailsByReturnId(local.id)
                    if (details.isNotEmpty()) {
                        supabase.from("returned_details").insert(details.map { it.toDto().copy(updated_at = currentTime) })
                    }
                    returnedDao.markAsSynced(local.id)
                }

                // الحالة 2: السيرفر أحدث -> اجلب للموبايل (Pull)
                remoteTime > local.updatedAt -> {
                    // 1. تحديث الرأس محلياً
                    returnedDao.insert(remote.toEntity().copy(
                        isSynced = true,
                        updatedAt = remoteTime
                    ))

                    // 2. جلب الأصناف من السيرفر (استخدام returned_id في الفلتر)
                    val remoteDetails = supabase.from("returned_details")
                        .select { filter { eq("returned_id", local.id) } } // ✅ تم التصحيح هنا
                        .decodeList<ReturnedDetailsDto>()

                    // 3. تحديث الأصناف محلياً (مسح ثم إضافة)
                    returnedDetailesDao.deleteReturnedDetails(local.id)

                    val localEntities = remoteDetails.map { it.toEntity().copy(updatedAt = remoteTime) }
                    returnedDetailesDao.insertReturnedDetails(localEntities) // ✅ تأكد من وجود هذا السطر

                    Log.d("Sync", "✅ تم تحديث المرتجع ${local.id} من السيرفر بنجاح")
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "Error Returned Sync ${local.id}: ${e.message}")
            FileLogger.logError("Returned Sync Fail", e)
        }
    }

    private suspend fun handleTransferSync(local: TransferEntity, currentTime: Long) {
        try {
            // 1. محاولة جلب التحويل من السيرفر للمقارنة
            val remote = supabase.from("transfers").select {
                filter { eq("id", local.id) }
            }.decodeSingleOrNull<TransferDto>()

            val remoteTime = remote?.updated_at ?: 0L

            when {
                // الحالة 1: الموبايل أحدث أو غير موجود بالسيرفر -> Push (رفع)
                remote == null || local.updatedAt > remoteTime -> {
                    Log.d("Sync", "رفع التحويل للسيرفر: ${local.id}")

                    // حذف الرأس القديم (لتجنب Duplicate Key) ثم إضافته
                    supabase.from("transfers").upsert(local.toDto().copy(updated_at = currentTime))

                    // حذف التفاصيل القديمة في السيرفر لضمان مطابقة الموبايل
                    supabase.from("transfer_details").delete {
                        filter { eq("transfer_id", local.id) }
                    }

                    // جلب التفاصيل من قاعدة البيانات المحلية
                    val details = transferDetailsDao.getDetailsByTransferIdSync(local.id)
                    if (details.isNotEmpty()) {
                        // رفع التفاصيل الجديدة
                        supabase.from("transfer_details").insert(details.map {
                            it.toDto().copy(updated_at = currentTime)
                        })
                    }

                    // تأشير المزامنة محلياً
                }

                // الحالة 2: السيرفر أحدث (تعديل من الإدارة مثلاً) -> Pull (سحب)
                remoteTime > local.updatedAt -> {
                    Log.d("Sync", "السيرفر أحدث، جاري تحديث التحويل محلياً: ${local.id}")

                    // 1. تحديث رأس التحويل في الـ Room
                    transferDetailsDao.insertTransfer(remote.toEntity().copy(
                        isSynced = true,
                        updatedAt = remoteTime
                    ))

                    // 2. جلب التفاصيل الجديدة من السيرفر
                    val remoteDetails = supabase.from("transfer_details")
                        .select { filter { eq("transfer_id", local.id) } }
                        .decodeList<TransferDetailsDto>()

                    // 3. تحديث التفاصيل محلياً (مسح القديم وإضافة القادم من السيرفر)
                    transferDetailsDao.deleteTransferDetailsByParentId(local.id)
                    val localEntities = remoteDetails.map {
                        it.toEntity().copy(updatedAt = remoteTime)
                    }
                    transferDetailsDao.insertTransferDetailList(localEntities)

                    Log.d("Sync", "✅ تم تحديث التحويل محلياً بنجاح")
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "خطأ في مزامنة التحويل ${local.id}: ${e.message}")
            FileLogger.logError("Transfer Sync Fail", e)
        }
    }


    override suspend fun syncWithConflictResolution() {
        try {
            // جلب كل الفواتير المحلية (المزامنة وغير المزامنة) للمراجعة
            val localOutbounds = outboundDao.getAllOutbound() // دالة تجلب قائمة عادية وليس Flow

            localOutbounds.forEach { localOutbound ->
                try {
                    // 1. محاولة جلب نسخة الفاتورة من السيرفر للمقارنة
                    val remoteOutbound = supabase.from("outbound")
                        .select {
                            filter { eq("id", localOutbound.id) }
                        }.decodeSingleOrNull<OutboundDto>()

                    val localTime = localOutbound.updatedAt // تأكد أن هذا الحقل موجود بملي ثانية
                    val remoteTime = remoteOutbound?.updated_at ?: 0L

                    when {
                        // الحالة: الموبايل أحدث أو السيرفر فارغ -> ارفع للسيرفر
                        remoteOutbound == null || localTime > remoteTime -> {
                            Log.d("Sync", "رفع التعديلات للسيرفر للفاتورة: ${localOutbound.id}")

                            // 1. رفع رأس الفاتورة
                            supabase.from("outbound").upsert(localOutbound.toDto())

                            // 2. جلب الأصناف الحالية الموجودة في الموبايل
                            val localDetails = detailsDao.getDetailsByOutboundIdStatic(localOutbound.id)

                            // 3. (الخطوة الأهم) حذف كافة الأصناف القديمة من السيرفر لهذه الفاتورة
                            // هذا يضمن أن أي صنف حذفه المندوب سيختفي من السيرفر أيضاً
                            supabase.from("outbound_details").delete {
                                filter {
                                    eq("outbound_id", localOutbound.id)
                                }
                            }

                            // 4. رفع الأصناف الجديدة (الموجودة حالياً في الموبايل)
                            if (localDetails.isNotEmpty()) {
                                val detailsDtos = localDetails.map { it.toDto() }
                                supabase.from("outbound_details").insert(detailsDtos)
                            }

                            outboundDao.markAsSynced(localOutbound.id)
                        }
                    }

                } catch (e: Exception) {
                    Log.e("Sync", "خطأ في معالجة الفاتورة ${localOutbound.invorseNumber}: ${e.message}")
                    FileLogger.logError("Conflict Resolution Error", e)
                }
            }
        } catch (e: Exception) {
            FileLogger.logError("Critical Sync Error", e)
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
                        val detailEntities = remoteDetails.map { it.toEntity(outbound.id)}
                        detailsDao.insertDetails(detailEntities)
                    }
                }
                Log.d("SyncCheck", "Downloaded ${remoteOutbounds.size} invoices and their details")
            }
        } catch (e: Exception) {
            Log.e("SyncCheck", "Failed to download outbounds: ${e.message}")
            FileLogger.logError("خطأ ة", e)

        }
    }

    override fun getOutboundDetails(outboundId: String): Flow<List<OutboundDetailWithItemName>> {
       return detailsDao.getDetailsListByOutboundId(outboundId)
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
    @Transaction
    override suspend fun updateInvoice(outbound: Outbound, details: List<OutboundDetails>) {
        val currentTime = System.currentTimeMillis()

        // 1. جلب الأصناف القديمة "قبل الحذف" لإرجاع الكميات للمخزن
        // ملاحظة: استخدم دالة تعيد List مباشرة وليس Flow أو First() لأننا داخل suspend function
        val oldDetails = detailsDao.getDetailsByOutboundIdStatic(outbound.id)

        oldDetails.forEach { oldDetail ->
            // إرجاع الكمية القديمة للمخزن (زائد)
            stockDao.updateStockQuantity(oldDetail.itemId, oldDetail.amount)
        }

        // 2. حذف الأصناف القديمة من جدول التفاصيل محلياً
        detailsDao.deleteDetailsByOutboundId(outbound.id)

        // 3. تحديث رأس الفاتورة بوقت جديد وحالة "غير متزامن" لضمان رفعه للسيرفر
        val updatedOutbound = outbound.copy(
            updatedAt = currentTime,
            isSynced = false // مهم جداً لكي تراه دالة المزامنة
        )
        outboundDao.update(updatedOutbound.toEntity())

        // 4. إضافة الأصناف الجديدة وخصم كمياتها من المخزن
        val detailsEntities = details.map { detail ->
            // خصم الكمية الجديدة من المخزن (ناقص)
            stockDao.updateStockQuantity(detail.itemId, -detail.amount)

            // تأكد من إعطاء ID جديد لو كان الصنف مضافاً حديثاً
            val finalId = if (detail.id.isEmpty()) java.util.UUID.randomUUID().toString() else detail.id
            detail.copy(id = finalId, updatedAt = currentTime).toEntity(outbound.id)
        }

        detailsDao.insertDetails(detailsEntities)
    }

}


