package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.core.scheduleSync
import com.example.myapplication.data.local.dao.InboundDao
import com.example.myapplication.data.local.dao.InboundDetailesDao
import com.example.myapplication.data.local.dao.ItemsDao
import com.example.myapplication.data.local.dao.StockDao
import com.example.myapplication.data.local.dao.SuppliedDao
import com.example.myapplication.data.local.entity.InboundDetailWithItemName
import com.example.myapplication.data.local.entity.ItemsEntity

import com.example.myapplication.data.local.entity.StockEntity
import com.example.myapplication.data.local.entity.Supplied
import com.example.myapplication.data.remote.dto.InboundDetailsDto
import com.example.myapplication.data.remote.dto.InboundDto
import com.example.myapplication.data.remote.dto.SuppliedDto
import com.example.myapplication.data.remote.manager.supabase
import com.example.myapplication.domin.model.InboundDetails
import com.example.myapplication.domin.repository.IInboundRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.myapplication.data.toDomain
import com.example.myapplication.data.toDto
import com.example.myapplication.data.toEntity
import com.example.myapplication.domin.model.Inbound
import com.example.myapplication.domin.model.Items
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.model.SuppliedModel
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.withContext

// InboundRepositoryImpl.kt
class InboundRepositoryImpl(
    private val inboundDao: InboundDao,
    private val detailsDao: InboundDetailesDao,
    private val stockDao: StockDao,
    private val suppliedDao: SuppliedDao,
    private val itemsDao: ItemsDao
) : IInboundRepository {
    override fun getAllInbounds(userId: String): Flow<List<Inbound>> =
        inboundDao.getAllInboundWithSupplierName(userId).map { list ->
            list.map { it.toDomain() } // دالة تحويل من Entity لـ Domain
        }

    override fun getStock(): Flow<List<Stock>> =
        stockDao.getAllStockWithNames().map { list ->
            list.map { it.toDomain() }
        }
    override fun getAllSupplied(): Flow<List<SuppliedModel>> =
        suppliedDao.getAllSupplied().map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun checkItemExists(itemId: Int): Boolean {
        return itemsDao.getItemById(itemId) != null
    }

    override suspend fun saveInboundTransaction(
        inbound: Inbound,
        details: List<InboundDetails>
    ): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val isUpdate = inbound.id > 0

                if (isUpdate) {
                    // 1. جلب الأصناف القديمة للفاتورة قبل حذفها
                    val oldDetails = detailsDao.getDetailsByInboundIdSync(inbound.id)

                    // 2. عكس تأثير الأصناف القديمة على المخزن (طرح الكميات القديمة)
                    for (oldDetail in oldDetails) {
                        // نستخدم القيمة بالسالب لعكس الإضافة السابقة
                        stockDao.updateStockQuantity(oldDetail.ItemId, -oldDetail.amount)
                    }

                    // 3. الآن احذف التفاصيل القديمة بأمان
                    detailsDao.deleteDetailsByInboundId(inbound.id.toLong())
                }

                // 4. حفظ أو تحديث الفاتورة الرئيسية
                val savedInboundId = inboundDao.insert(inbound.toEntity().copy(isSynced = false)).toInt()
                val finalInboundId = if (isUpdate) inbound.id else savedInboundId

                // 5. إضافة التفاصيل الجديدة وتحديث المخزن بالكميات الجديدة
                for (newDetail in details) {
                    detailsDao.insert(newDetail.toEntity(finalInboundId).copy(isSynced = false))

                    // تحديث المخزن بالكمية الجديدة (إضافة)
                    val rows = stockDao.updateStockQuantity(newDetail.ItemId, newDetail.amount)
                    if (rows == 0) {
                        stockDao.insert(
                            StockEntity(
                                ItemId = newDetail.ItemId,
                                CurrentAmount = newDetail.amount,
                                isSynced = false,
                                userId = newDetail.userId,
                            )
                        )
                    }
                }

            }
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("RepoError", "Update Stock Error: ${e.message}")
            Result.failure(e)
        }
    }
    override fun getAllItems(): Flow<List<Items>> {
        return itemsDao.getAllItems().map { list ->
            list.map { it.toDomain() }
        }
    }
    suspend fun syncUnsynced(): Result<Unit> {
        return try {
            val unsyncedInbounds = inboundDao.getUnsyncedInbounds()

            for (inbound in unsyncedInbounds) {
                // تحديث وقت التعديل قبل الإرسال لضمان مزامنة التوقيت
                val currentTime = System.currentTimeMillis()
                val inboundWithTime = inbound.copy(updatedAt = currentTime)

                // 1. استخدام upsert للفاتورة (سيقوم بتحديث الـ updated_at في Supabase)
                val response = supabase.from("inbound").upsert(inboundWithTime.toDto()) {
                    select()
                }.decodeSingle<InboundDto>()

                val serverInboundId = response.id

                // 2. جلب التفاصيل وتحديث توقيتها أيضاً
                val details = detailsDao.getDetailsByInboundIdSync(inbound.id)

                // 3. مسح التفاصيل القديمة في السيرفر لضمان مطابقة التعديل الأخير
                supabase.from("inbound_details").delete {
                    filter { eq("inbound_id", serverInboundId) }
                }

                // 4. رفع التفاصيل الجديدة مع توقيت التحديث الجديد
                if (details.isNotEmpty()) {
                    val detailsDtos = details.map {
                        it.toDto().copy(
                            inbound_id = serverInboundId,
                            updated_at = currentTime // نفس وقت تحديث الفاتورة الرئيسية
                        )
                    }
                    supabase.from("inbound_details").upsert(detailsDtos)
                }

                // 5. تحديث الحالة محلياً
                inboundDao.insert(inboundWithTime.copy(isSynced = true))
                details.forEach {
                    detailsDao.insert(it.copy(isSynced = true, updatedAt = currentTime))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SyncError", "فشل المزامنة: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun addInSupbase(inbound: Inbound, details: List<InboundDetails>) {
        withContext(Dispatchers.IO + NonCancellable) { // هذا السطر يمنع إلغاء العملية
            syncUnsynced()
        }
    }
    override suspend fun deleteInbound(inbound: Inbound) {
        inboundDao.delete(inbound.toEntity())
        // إذا أردت حذفها من Supabase أيضاً:
         supabase.from("inbound").delete { filter { eq("id", inbound.id) } }
    }
    override suspend fun deleteFullInbound(inbound: Inbound): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                // 1. جلب تفاصيل الفاتورة من قاعدة البيانات المحلية قبل حذفها
                // نستخدم id الفاتورة للوصول لكل الأصناف المرتبطة بها
                val detailsToDelete = detailsDao.getDetailsByInboundIdSync(inbound.id)

                // 2. تحديث المخزن (خصم الكميات التي كانت موجودة في الفاتورة)
                for (detail in detailsToDelete) {
                    // نمرر الكمية بالسالب لخصمها من الرصيد الحالي في المخزن
                    stockDao.updateStockQuantity(detail.ItemId, -detail.amount)
                }

                // 3. الحذف من Supabase (السحاب)
                // نحذف التفاصيل أولاً ثم الفاتورة لتجنب مشاكل العلاقات (Foreign Key)
                supabase.from("inbound_details").delete {
                    filter { eq("inbound_id", inbound.id) }
                }
                supabase.from("inbound").delete {
                    filter { eq("id", inbound.id) }
                }

                // 4. الحذف من Room (محلياً)
                detailsDao.deleteDetailsByInboundId(inbound.id.toLong())
                inboundDao.delete(inbound.toEntity())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RepoError", "Delete error: ${e.message}")
            Result.failure(e)
        }
    }    override suspend fun insertItemsList(entities: List<ItemsEntity>) {
        itemsDao.insertItemsList(entities)
    }

    override fun getDetailsByInboundId(inboundId: Long): Flow<List<InboundDetailWithItemName>> {
        return detailsDao.getDetailsByInboundId(inboundId)

    }
// داخل ملف InboundRepositoryImpl.kt

    suspend fun syncSuppliedFromServer() {
        try {
            // 1. جلب البيانات مباشرة من Supabase
            // لاحظ: decodeList ترجع القائمة مباشرة في الإصدارات الحديثة من مكتبة كوتلن سوبابيس
            val remoteSupplied = supabase.from("supplied") // تأكد من مطابقة اسم الجدول في سوبابيس (حساس لحالة الأحرف)
                .select()
                .decodeList<SuppliedDto>()

            if (remoteSupplied.isNotEmpty()) {
                // 2. اختيارياً: حذف البيانات القديمة لضمان عدم وجود بيانات ممسوحة من السيرفر
                // suppliedDao.deleteAll()

                // 3. تحويل من DTO (البيانات القادمة من السيرفر) إلى Entity (الخاصة بـ Room)
                val entities = remoteSupplied.map { dto ->
                    Supplied(
                        id = dto.id,
                        suppliedName = dto.supplied_name ?: "",
                        num = dto.num.toString()
                    )
                }

                // 4. حفظ البيانات في Room (يفضل استخدام insertAll إذا كانت متوفرة في DAO لسرعة الأداء)
                entities.forEach { entity ->
                    suppliedDao.insert(entity)
                }

                Log.d("SYNC_SUPPLIED", "تم تحديث ${entities.size} مورد/مخزن من Supabase")
            } else {
                Log.d("SYNC_SUPPLIED", "لا توجد بيانات في جدول Supplied على السيرفر")
            }

        } catch (e: Exception) {
            Log.e("SYNC_SUPPLIED", "خطأ أثناء المزامنة مع Supabase: ${e.localizedMessage}")
            throw e
        }
    }    // داخل InboundRepoImpl.kt
    override suspend fun syncInboundFromServer(userId: String) {
        try {
            val remoteInbounds = supabase.from("inbound")
                .select { filter { eq("user_id", userId) } }
                .decodeList<InboundDto>()

            remoteInbounds.forEach { remoteDto ->
                // جلب النسخة المحلية من الفاتورة
                val localInbound = inboundDao.getInboundByIdSync(remoteDto.id)

                // المزامنة فقط إذا كانت بيانات السيرفر أحدث أو إذا كانت الفاتورة غير موجودة محلياً
                // وإذا كانت الفاتورة المحلية "مزامنة بالفعل" (isSynced == true)
                if (localInbound == null || (remoteDto.updated_at ?: 0 > localInbound.updatedAt)) {
                    inboundDao.insert(remoteDto.toEntity().copy(isSynced = true))

                    // جلب التفاصيل وتحديثها أيضاً
                    val details = supabase.from("inbound_details")
                        .select { filter { eq("inbound_id", remoteDto.id) } }
                        .decodeList<InboundDetailsDto>()

                    if (details.isNotEmpty()) {
                        detailsDao.insertInboundDetailsList(details.map { it.toEntity().copy(isSynced = true) })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "خطأ في جلب البيانات الأحدث: ${e.message}")
        }
    }

}