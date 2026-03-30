package com.example.myapplication.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.dao.CustomerDao
import com.example.myapplication.data.local.dao.OutboundDetailesDao
import com.example.myapplication.data.local.dao.ReturnedDao
import com.example.myapplication.data.local.dao.ReturnedDetailsDao
import com.example.myapplication.data.local.dao.StockDao
import com.example.myapplication.data.local.entity.ItemHistoryProjection
import com.example.myapplication.data.local.entity.ItemsEntity
import com.example.myapplication.data.local.entity.ReturnedDetailsEntity
import com.example.myapplication.data.local.entity.ReturnedWithNames
import com.example.myapplication.data.remote.dto.ReturnedDto
import com.example.myapplication.data.remote.manager.supabase
import com.example.myapplication.data.toDto
import com.example.myapplication.data.toEntity
import com.example.myapplication.data.toReturnedDetailsModel
import com.example.myapplication.data.toReturnedEntity
import com.example.myapplication.data.toReturnedModel
import com.example.myapplication.domin.model.ReturnedDetailsModel
import com.example.myapplication.domin.model.ReturnedModel
import com.example.myapplication.domin.model.ReturnedWithNameModel
import com.example.myapplication.domin.repository.ReturnedRepo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReturnedRepoImpl(private val database: AppDatabase, // نحتاجه للـ Transaction
                       private val returnedDao: ReturnedDao,
                       private val returnedDetailsDao: ReturnedDetailsDao,
                       private val outboundDetailsDao: OutboundDetailesDao, // تأكد من إضافة الاستعلامات فيه
                       private val customerDao: CustomerDao,
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
            val returnedId = returnedDao.insert(returnedModel.toReturnedEntity())

            // 2. معالجة الأصناف المرتجعة صنفاً صنفاً
            returnedDetails.forEach { detailModel ->

                // أ- حفظ تفاصيل المرتجع
                val detailsEntity = ReturnedDetailsEntity(
                    id = 0, // تلقائي
                    returnedId = returnedId.toInt(),
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
    override suspend fun getAllReturnedDetailsByReturnedId(returnedId: Int): Flow<List<ReturnedDetailsModel>> {
        return returnedDetailsDao.getAllReturnedDetails(returnedId).map { it->
            it.map { item->
                item.toReturnedDetailsModel()

            }

        }
    }

    // داخل ReturnedRepoImpl
    override fun getAllReturnedDetails(returnedId: Int): Flow<List<ReturnedDetailsModel>> {
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
            val remoteReturns = supabase.from("returned")
                .select { filter { eq("user_id", userId) } }
                .decodeList<ReturnedDto>()

            if (remoteReturns.isNotEmpty()) {
                val entities = remoteReturns.map { it.toEntity() }
                returnedDao.insertReturnsList(entities) // تأكد من وجود هذه الدالة في الـ DAO
            }
        } catch (e: Exception) {
            Log.e("Sync", "Error fetching returns: ${e.message}")
        }
    }
    override fun getItemsByCustomer(customerId: Int): Flow<List<ItemsEntity>> {
        return outboundDetailsDao.getItemsBoughtByCustomer(customerId)
    }

    override suspend fun getLastPrice(customerId: Int, itemId: Int): Double? {
        return outboundDetailsDao.getLastSellingPrice(customerId, itemId)    }
}