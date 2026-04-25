package com.example.myapplication.data.repository

import android.util.Log
import androidx.room.Transaction
import androidx.room.withTransaction
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.dao.CustomerDao
import com.example.myapplication.data.local.dao.ItemsDao
import com.example.myapplication.data.local.dao.OutboundDetailesDao
import com.example.myapplication.data.local.dao.ReturnedDao
import com.example.myapplication.data.local.dao.ReturnedDetailsDao
import com.example.myapplication.data.local.dao.StockDao
import com.example.myapplication.data.local.entity.ItemHistoryProjection
import com.example.myapplication.data.local.entity.ItemsEntity
import com.example.myapplication.data.local.entity.ReturnedDetailsEntity
import com.example.myapplication.data.local.entity.ReturnedEntity
import com.example.myapplication.data.local.entity.ReturnedWithNames
import com.example.myapplication.data.remote.dto.InboundDetailsDto
import com.example.myapplication.data.remote.dto.ReturnedDetailsDto
import com.example.myapplication.data.remote.dto.ReturnedDto
import com.example.myapplication.data.toDto
import com.example.myapplication.data.toEntity
import com.example.myapplication.data.toReturnedDetailsModel
import com.example.myapplication.data.toReturnedEntity
import com.example.myapplication.data.toReturnedModel
import com.example.myapplication.domin.model.ReturnedDetailsModel
import com.example.myapplication.domin.model.ReturnedModel
import com.example.myapplication.domin.model.ReturnedWithNameModel
import com.example.myapplication.domin.repository.ReturnedRepo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReturnedRepoImpl @Inject constructor(private val database: AppDatabase, // نحتاجه للـ Transaction
                                           private val supabase: SupabaseClient,
                                           private val returnedDao: ReturnedDao,
                                           private val returnedDetailsDao: ReturnedDetailsDao,
                                           private val outboundDetailsDao: OutboundDetailesDao, // تأكد من إضافة الاستعلامات فيه
                                           private val customerDao: CustomerDao,
                                           private val itemDao: ItemsDao,

                                           private val stockDao: StockDao ):
    ReturnedRepo {
    override suspend fun insertReturned(
        returnedModel: ReturnedModel,
        returnedDetails: List<ReturnedDetailsModel>,
        debtAmount: Double


        ) {
        // استخدام Transaction لضمان أنه إذا فشل تحديث صنف واحد، تُلغى العملية كاملة
        database.withTransaction {

            // 1. حفظ رأس الفاتورة (ReturnedEntity)
            returnedDao.insert(returnedModel.toReturnedEntity())

            // 2. معالجة الأصناف المرتجعة صنفاً صنفاً
            returnedDetails.forEach { detailModel ->

                // أ- حفظ تفاصيل المرتجع
                val detailsEntity = ReturnedDetailsEntity(
                    id = java.util.UUID.randomUUID().toString(), // تلقائي
                    returnedId = returnedModel.id,
                    itemId = detailModel.itemId,
                    amount = detailModel.amount,
                    price = detailModel.price // يمكنك تمرير السعر إذا كان مخزناً في الموديل
                )
                returnedDetailsDao.addReturned(detailsEntity)

                // ب- تحديث المخزن (زيادة الكمية المرتجعة)
                stockDao.updateStockQuantity(detailModel.itemId, detailModel.amount)

                customerDao.decreaseCustomerBalance(returnedModel.customerId,debtAmount)
            }
        }
    }

    override fun getAllReturned(): Flow<List<ReturnedWithNameModel>> {
        return returnedDao.getAllReturnedWithNames().map { list ->
            list.map { item ->
                ReturnedWithNameModel(
                    returnedModel = item.returned.toReturnedModel(), // موديل البيانات الأساسية
                    customerName = item.customerName?:"", // الاسم القادم من الـ Join
                    itemName = item.itemName?:"",         // الاسم القادم من الـ Join
                    totalPrice = 0.0 // احسبه أو جلب قيمته
                )
            }
        }
    }
    override suspend fun getAllReturnedDetailsByReturnedId(returnedId: String): Flow<List<ReturnedDetailsModel>> {
        return returnedDetailsDao.getAllReturnedDetails(returnedId).map { it->
            it.map { item->
                item.toReturnedDetailsModel()

            }

        }
    }

    // داخل ReturnedRepoImpl
    override fun getAllReturnedDetails(returnedId: String): Flow<List<ReturnedDetailsModel>> {
        return returnedDetailsDao.getDetailsByReturnedId(returnedId).map { list ->
            list.map { item ->
                ReturnedDetailsModel(
                    id = item.details.id,
                    returnedId = item.details.returnedId,
                    itemId = item.details.itemId,
                    itemName = item.itemName, // تأكد من إضافة هذا الحقل في الموديل
                    amount = item.details.amount,
                    price = item.details.price
                )
            }
        }
    }

    override fun getItemPurchaseHistory(customerId: Int, itemId: Int): Flow<List<ItemHistoryProjection>> {
        // نستخدم الـ DAO الخاص بالمبيعات (Outbound) لجلب البيانات
        return outboundDetailsDao.getItemPurchaseHistory(customerId, itemId)
    }

    override suspend fun syncReturnsWithServer() {
        try {
            // 1. جلب المرتجعات غير المزامنة من Room
            val unsyncedReturns = returnedDao.getUnsyncedReturns()

            unsyncedReturns.forEach { entity ->
                // أ- رفع رأس المرتجع والحصول على الـ ID من السيرفر
                val remoteReturn = supabase.from("returned")
                    .insert(entity.toDto()) { select() }
                    .decodeSingle<ReturnedDto>()

                // ب- جلب تفاصيل هذا المرتجع ورفعها بالسيرفر باستخدام الـ ID الجديد
                val details = returnedDetailsDao.getDetailsByReturnId(entity.id)
                val detailsDtos = details.map {
                    it.toDto().copy(returned_id = remoteReturn.id!!)
                }

                if (detailsDtos.isNotEmpty()) {
                    supabase.from("returned_details").insert(detailsDtos)
                }

                // ج- تحديث الحالة محلياً كـ "تمت المزامنة"
                returnedDao.markAsSynced(entity.id)
            }
        } catch (e: Exception) {
            Log.e("SyncError", "Failed to sync returns: ${e.message}")
        }
    }
    // داخل ReturnedRepoImpl.kt

    override suspend fun syncReturnsFromServer(userId: String) {
        try {
            // 1. جلب كل المرتجعات الخاصة بالمستخدم من السيرفر
            val remoteReturns = supabase.from("returned")
                .select { filter { eq("user_id", userId) } }
                .decodeList<ReturnedDto>()

            remoteReturns.forEach { remoteDto ->
                // 2. جلب النسخة المحلية (لو موجودة) للتأكد من حالة التحديث

                // 3. شرط التحديث:
                // - الفاتورة مش موجودة أصلاً محلياً
                // - أو نسخة السيرفر أحدث من النسخة اللي عندي (بناءً على updatedAt)


                    // حفظ الرأس (Header) وتأكيد المزامنة
                    returnedDao.insert(remoteDto.toEntity().copy(isSynced = true))

                    // 4. جلب التفاصيل الخاصة بهذه الفاتورة تحديداً
                    // ملحوظة: تأكد من اسم العمود في السيرفر (returned_id أو return_id)
                val currentReturnId = remoteDto.id ?: return@forEach

// 2. جلب التفاصيل باستخدام الـ ID الخاص بالفاتورة الحالية فقط
                val details = supabase.from("returned_details")
                    .select {
                        filter {
                            // استخدم eq مع رقم واحد (currentReturnId) وليس مع القائمة كاملة
                            eq("returned_id", currentReturnId)
                        }
                    }
                    .decodeList<ReturnedDetailsDto>()

                    if (details.isNotEmpty()) {
                        // تحويل الـ DTOs لـ Entities وحفظها
                        returnedDetailsDao.insertReturnedDetailsList(
                            details.map { it.toEntity() }
                        )
                    }
                    Log.d("Sync", "تم تحديث الفاتورة رقم ${remoteDto.id} وتفاصيلها")

            }
        } catch (e: Exception) {
            Log.e("Sync", "خطأ في مزامنة المرتجعات: ${e.message}")
        }
    }

    override suspend fun deleteReturned(returned:  ReturnedWithNameModel) {
        returnedDao.delete(returned.returnedModel.toEntity())
        returnedDetailsDao.deleteReturnedDetails(returned.returnedModel.id)
    }

    override fun getItemsByCustomer(): Flow<List<ItemsEntity>> {
        return itemDao.getAllItems()
    }

    @Transaction
    override suspend fun updateReturned(returned: ReturnedModel, details: List<ReturnedDetailsModel>, newDebtAmount: Double) {
        // 1. جلب بيانات المرتجع القديم (قبل التعديل) لمعرفة العميل القديم والمبلغ القديم
        val oldReturnedHeader = returnedDao.getReturnedByIdStatic(returned.id)
        val oldDetails = returnedDetailsDao.getReturnedDetailsStatic(returned.id)

        // حساب إجمالي المرتجع القديم
        val oldTotalAmount = oldDetails.sumOf { it.amount * it.price }

        // ==========================================
        // الخطوة 1: إلغاء أثر المرتجع القديم تماماً
        // ==========================================

        // أ- خصم الكميات من المخزن (لأن المرتجع كان زودها)
        oldDetails.forEach { old ->
            stockDao.reduceStock(old.itemId, old.amount)
        }

        // ب- تعديل مديونية العميل القديم (نطرح منه قيمة المرتجع القديم ليرجع حسابه كما كان)
        // ملاحظة: المرتجع بيقلل المديونية، فعشان نلغيه لازم "نزود" المديونية تاني بالفرق
        customerDao.updateCustomerDebt(oldReturnedHeader.customerId, -oldTotalAmount)

        // ==========================================
        // الخطوة 2: تطبيق بيانات المرتجع الجديد
        // ==========================================

        // أ- حذف التفاصيل القديمة من قاعدة البيانات
        returnedDetailsDao.deleteReturnedDetails(returned.id)

        // ب- زيادة المخزن بالكميات الجديدة
        details.forEach { new ->
            stockDao.updateStockQuantity(new.itemId, new.amount)
        }

        // ج- إضافة التفاصيل الجديدة
        val detailsEntities = details.map { it.toEntity(returned.id) }
        returnedDetailsDao.insertReturnedDetails(detailsEntities)

        // د- تحديث حساب العميل الجديد (إضافة قيمة المرتجع الجديد لميزانه)
        // المديونية بتقل بقيمة المرتجع الجديد
        val newTotalAmount = details.sumOf { it.amount * it.price }
        customerDao.updateCustomerDebt(returned.customerId, newTotalAmount)

        returnedDao.update(returned.toEntity())
    }

    override suspend fun getLastPrice(customerId: Int, itemId: Int): Double? {
        return outboundDetailsDao.getLastSellingPrice(customerId, itemId)    }
}