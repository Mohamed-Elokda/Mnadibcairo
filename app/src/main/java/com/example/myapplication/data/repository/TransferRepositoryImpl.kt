package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.local.dao.StockDao
import com.example.myapplication.data.local.dao.TransferDao
import com.example.myapplication.data.local.entity.TransferEntity
import com.example.myapplication.data.remote.dto.ReturnedDto
import com.example.myapplication.data.remote.dto.TransferDetailsDto
import com.example.myapplication.data.remote.dto.TransferDto
import com.example.myapplication.data.toDto
import com.example.myapplication.data.toEntity
import com.example.myapplication.domin.model.Transfer
import com.example.myapplication.domin.model.TransferDetailWithItemName
import com.example.myapplication.domin.model.TransferDetails
import com.example.myapplication.domin.model.TransferWithStoreName
import com.example.myapplication.domin.repository.ITransferRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject


class TransferRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val transferDao: TransferDao,
    private val stockDao: StockDao
) : ITransferRepository {

    override suspend fun saveTransfer(
        transfer: Transfer,
        details: List<TransferDetails>
    ): Result<Unit> {
        return try {
            // تنفيذ العملية كعملية واحدة (Transaction)
            val tId = transferDao.insertTransfer(transfer.toEntity()).toInt()

            details.forEach { detail ->
                transferDao.insertTransferDetail(detail.copy(transferId = transfer.id).toEntity())

                // 1. الخصم من المخزن المصدر (مثل الصادر)
                stockDao.updateStockQuantity(detail.itemId, -detail.amount)

                // 2. الإضافة للمخزن الوجهة (مثل الوارد)
                // ملحوظة: إذا كان جدول الـ Stock يدعم IDs المخازن، مرر الـ toStoreId هنا
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // في الـ Implementation
    override fun getTransferDetails(transferId: String): Flow<List<TransferDetailWithItemName>> {
        return transferDao.getTransferDetailsWithNames(transferId)
    }

    override fun getAllTransfers(userId: String): Flow<List<TransferWithStoreName>> {
        return transferDao.getAllTransfersWithStoreName(userId)
    }

    override suspend fun deleteFullTransfer(transferId: String) {
        transferDao.deleteTransferDetailsByParentId(transferId)
        transferDao.deleteTransferById(transferId)
    }


    override suspend fun syncTransferFromServer(userId: String) {
        try {
            // 1. جلب رؤوس المناقلات
            val remoteTransfers = supabase.from("transfers")
                .select { filter { eq("user_id", userId) } }
                .decodeList<TransferDto>()

            if (remoteTransfers.isNotEmpty()) {
                val transferEntities = remoteTransfers.map { it.toEntity() }
                transferDao.insertTransferList(transferEntities)

                // 2. تجميع الـ IDs (تأكد من تنظيفها من الـ null)
                val transferIds = remoteTransfers.map { it.id }.filterNotNull()

                // 3. جلب تفاصيل المناقلات (التعديل هنا)
                val remoteDetails = supabase.from("transfer_details")
                    .select {
                        filter {
                            // استخدام isIn بدلاً من filter(FilterOperator.IN)
                            isIn("transfer_id", transferIds)
                        }
                    }
                    .decodeList<TransferDetailsDto>()

                // 4. حفظ التفاصيل
                if (remoteDetails.isNotEmpty()) {
                    val detailEntities = remoteDetails.map { it.toEntity() }
                    transferDao.insertTransferDetailList(detailEntities)
                }

                Log.d("Sync", "تمت مزامنة ${remoteTransfers.size} مناقلة بنجاح.")
            }
        } catch (e: Exception) {
            Log.e("Sync", "Error fetching transfers: ${e.message}")
        }
    }
}