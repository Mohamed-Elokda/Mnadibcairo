package com.example.myapplication.data.repository

import android.content.Context
import android.util.Log
import androidx.room.Transaction
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
import com.example.myapplication.data.remote.dto.OutboundDto
import com.example.myapplication.data.remote.dto.SuppliedDto
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
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.map

// InboundRepositoryImpl.kt
class InboundRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context, // أضف هذا
    private val inboundDao: InboundDao,
    private val detailsDao: InboundDetailesDao,
    private val stockDao: StockDao,
    private val suppliedDao: SuppliedDao,
    private val itemsDao: ItemsDao,
    private val supabase: SupabaseClient,

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
    @Transaction
    override suspend fun saveInboundTransaction(
        inbound: Inbound,
        details: List<InboundDetails>
    ): Result<Unit> {
        return try {
            // نستخدم database.withTransaction لضمان تنفيذ كل العمليات أو إلغائها معاً


                // 1. تحديد الـ ID النهائي (سواء جديد أو تحديث)
                val finalInboundId = if (inbound.id.isNullOrEmpty()) {
                    java.util.UUID.randomUUID().toString()
                } else {
                    inbound.id
                }

                val isUpdate = inbound.id.isNotEmpty()
                val currentTime = System.currentTimeMillis()

                if (isUpdate) {
                    // 2. جلب الأصناف القديمة لعكس تأثيرها على المخزن
                    val oldDetails = detailsDao.getDetailsByInboundIdSync(finalInboundId)
                    for (oldDetail in oldDetails) {
                        // طرح الكمية القديمة من المخزن قبل الحذف
                        stockDao.updateStockQuantity(oldDetail.ItemId, -oldDetail.amount)
                    }
                    // 3. حذف التفاصيل القديمة
                    detailsDao.deleteDetailsByInboundId(finalInboundId)
                }

                // 4. حفظ الفاتورة الرئيسية (مع تحديث وقت التعديل للمزامنة)
                inboundDao.insert(
                    inbound.toEntity().copy(
                        id = finalInboundId,
                        isSynced = false,
                        updatedAt = currentTime // مهم جداً للمزامنة الذكية
                    )
                )

                // 5. إضافة التفاصيل الجديدة وتحديث المخزن
                for (newDetail in details) {
                    detailsDao.insert(
                        newDetail.toEntity(finalInboundId).copy(
                            isSynced = false,
                            updatedAt = currentTime
                        )
                    )

                    // تحديث المخزن (إضافة الكمية الجديدة)
                    val rows = stockDao.updateStockQuantity(newDetail.ItemId, newDetail.amount)
                    if (rows == 0) {
                        stockDao.insert(
                            StockEntity(
                                ItemId = newDetail.ItemId,
                                CurrentAmount = newDetail.amount,
                                isSynced = false,
                                userId = inbound.userId, // تأكد من استلام الـ userId من الفاتورة
                                updatedAt = currentTime
                            )
                        )
                    }

            }
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("RepoError", "Update Stock Error: ${e.message}")
            FileLogger.logError("خطأ في حفظ الوارد", e)
            Result.failure(e)
        }
    }

    override fun getAllItems(): Flow<List<Items>> {
        return itemsDao.getAllItems().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun syncWithConflictResolution() {
        try {
            // جلب كل الفواتير المحلية (المزامنة وغير المزامنة) للمراجعة
            val localOutbounds = inboundDao.getAllOutbound() // دالة تجلب قائمة عادية وليس Flow

            localOutbounds.forEach { localOutbound ->
                try {
                    // 1. محاولة جلب نسخة الفاتورة من السيرفر للمقارنة
                    val remoteOutbound = supabase.from("inbound")
                        .select {
                            filter { eq("id", localOutbound.id) }
                        }.decodeSingleOrNull<InboundDto>()

                    val localTime = localOutbound.updatedAt // تأكد أن هذا الحقل موجود بملي ثانية
                    val remoteTime = remoteOutbound?.updated_at ?: 0L

                    when {
                        // الحالة: الموبايل أحدث أو السيرفر فارغ -> ارفع للسيرفر
                        remoteOutbound == null || localTime > remoteTime -> {
                            Log.d("Sync", "رفع التعديلات للسيرفر للفاتورة: ${localOutbound.id}")

                            // 1. رفع رأس الفاتورة
                            supabase.from("inbound").upsert(localOutbound.toDto())

                            // 2. جلب الأصناف الحالية الموجودة في الموبايل
                            val localDetails = detailsDao.getDetailsByOutboundIdStatic(localOutbound.id)

                            // 3. (الخطوة الأهم) حذف كافة الأصناف القديمة من السيرفر لهذه الفاتورة
                            // هذا يضمن أن أي صنف حذفه المندوب سيختفي من السيرفر أيضاً
                            supabase.from("inbound_details").delete {
                                filter {
                                    eq("inbound_id", localOutbound.id)
                                }
                            }

                            // 4. رفع الأصناف الجديدة (الموجودة حالياً في الموبايل)
                            if (localDetails.isNotEmpty()) {
                                val detailsDtos = localDetails.map { it.toDto() }
                                supabase.from("inbound_details").insert(detailsDtos)
                            }

//                            inboundDao.markAsSynced(localOutbound.id)
                        }
                    }

                } catch (e: Exception) {
                    Log.e("Sync", "خطأ في معالجة الفاتورة ${localOutbound.invorseNum}: ${e.message}")
                    FileLogger.logError("Conflict Resolution Error", e)
                }
            }
        } catch (e: Exception) {
            FileLogger.logError("Critical Sync Error", e)
        }
    }

    override suspend fun syncUnsynced() {
         try {
            val unsyncedInbounds = inboundDao.getUnsyncedInbounds()
            unsyncedInbounds.forEach { inbound ->
                try {

                    // تحديث وقت التعديل محلياً للـ Inbound
                    val updatedInbound = inbound.copy(updatedAt = System.currentTimeMillis())

                    // رفع رأس فاتورة الوارد
                    supabase.from("inbound").upsert(updatedInbound.toDto())

                    // جلب تفاصيل الوارد
                    val inDetails = detailsDao.getDetailsByOutboundIdStatic(inbound.id)
                    if (inDetails.isNotEmpty()) {
                        // مسح القديم في السيرفر لضمان عدم التكرار أو النقص (اختياري حسب منطق الـ PK عندك)
                        supabase.from("inbound_details").delete {
                            filter { eq("inbound_id", inbound.id) }
                        }

                        val detailsDtos = inDetails.map {
                            it.toDto().copy(updated_at = System.currentTimeMillis())
                        }
                        supabase.from("inbound_details").upsert(detailsDtos)
                    }

                    // تحديث الحالة محلياً
                    inboundDao.insert(updatedInbound.copy(isSynced = true))
                    inDetails.forEach {
                        detailsDao.insert(it.copy(isSynced = true, updatedAt = System.currentTimeMillis()))
                    }

                    Log.d("Sync", "تمت مزامنة الوارد رقم ${inbound.id} بنجاح")
                } catch (e: Exception) {
                    Log.e("Sync", "فشل مزامنة الوارد ${inbound.id}: ${e.message}")
                    FileLogger.logError("خطأ مزامنة وارد", e)
                }
            }
        }
        catch (e: Exception) {
                Log.e("Sync", "فشل مزامنة الوارد $: ${e.message}")
                FileLogger.logError("خطأ مزامنة وارد", e)
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
                detailsDao.deleteDetailsByInboundId(inbound.id)
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

    override fun getDetailsByInboundId(inboundId: String): Flow<List<InboundDetailWithItemName>> {
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
                if (localInbound == null || ((remoteDto.updated_at ?: 0) > localInbound.updatedAt)) {
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