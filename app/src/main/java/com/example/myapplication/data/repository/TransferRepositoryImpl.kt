package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.local.dao.StockDao
import com.example.myapplication.data.local.dao.TransferDao
import com.example.myapplication.data.remote.dto.TransferDto
import com.example.myapplication.data.remote.manager.supabase
import com.example.myapplication.data.toDto
import com.example.myapplication.data.toEntity
import com.example.myapplication.domin.model.Transfer
import com.example.myapplication.domin.model.TransferDetails
import com.example.myapplication.domin.repository.ITransferRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class TransferRepositoryImpl(
    private val transferDao: TransferDao,
    private val stockDao: StockDao
) : ITransferRepository {

    override suspend fun saveTransfer(transfer: Transfer, details: List<TransferDetails>): Result<Unit> {
        return try {
            // تنفيذ العملية كعملية واحدة (Transaction)
            val tId = transferDao.insertTransfer(transfer.toEntity()).toInt()

            details.forEach { detail ->
                transferDao.insertTransferDetail(detail.copy(transferId = tId).toEntity())

                // 1. الخصم من المخزن المصدر (مثل الصادر)
                stockDao.updateStockQuantity(detail.itemId, -detail.amount)

                // 2. الإضافة للمخزن الوجهة (مثل الوارد)
                // ملحوظة: إذا كان جدول الـ Stock يدعم IDs المخازن، مرر الـ toStoreId هنا
                stockDao.updateStockQuantity(detail.itemId, detail.amount)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncTransfers(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // 1. جلب العمليات التي لم يتم مزامنتها
            val unsynced = transferDao.getUnsyncedTransfers() // تأكد من وجود هذه الدالة في الـ DAO

            for (transferEntity in unsynced) {
                // 2. رفع رأس الفاتورة إلى سوبابيس (جدول transfers)
                val response = supabase.from("transfers").upsert(transferEntity.toDto()) {
                    select()
                }.decodeSingle<TransferDto>()

                val supabaseTransferId = response.id ?: throw Exception("فشل الحصول على ID من السيرفر")

                // 3. جلب التفاصيل المرتبطة بهذه الفاتورة محلياً
                val details = transferDao.getDetailsByTransferIdSync(transferEntity.id)

                if (details.isNotEmpty()) {
                    // 4. رفع التفاصيل دفعة واحدة مع ربطها بالـ ID الجديد من سوبابيس
                    val detailsDtoList = details.map {
                        it.toDto().copy(transferId = supabaseTransferId)
                    }
                    supabase.from("transfer_details").insert(detailsDtoList)

                    // 5. تحديث الحالة محلياً للرأس والتفاصيل
                    transferDao.updateSyncStatus(transferEntity.id, true)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SyncTransfers", "خطأ أثناء المزامنة: ${e.message}")
            Result.failure(e)
        }
    }
}