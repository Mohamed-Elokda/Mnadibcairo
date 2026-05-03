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
import com.example.myapplication.data.local.dao.PaymentDao
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
import com.example.myapplication.data.local.entity.PaymentEntity
import com.example.myapplication.data.local.entity.ReturnedEntity
import com.example.myapplication.data.local.entity.TransferEntity
import com.example.myapplication.data.remote.dto.CustomerDto
import com.example.myapplication.data.remote.dto.InboundDetailsDto
import com.example.myapplication.data.remote.dto.InboundDto
import com.example.myapplication.data.remote.dto.ItemsDto
import com.example.myapplication.data.remote.dto.OutboundDetailsDto
import com.example.myapplication.data.remote.dto.OutboundDto
import com.example.myapplication.data.remote.dto.PaymentDto
import com.example.myapplication.data.remote.dto.ReturnedDetailsDto
import com.example.myapplication.data.remote.dto.ReturnedDto
import com.example.myapplication.data.remote.dto.SaveInboundParams
import com.example.myapplication.data.remote.dto.SaveOutboundParams
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
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OutboundRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient,
    private val outboundDao: OutboundDao,
    private val detailsDao: OutboundDetailesDao,
    private val paymentDao: PaymentDao,
    private val inboundDetailesDao: InboundDetailesDao,
    private val returnedDao: ReturnedDao,
    private val returnedDetailesDao: ReturnedDetailsDao,
    private val transferDetailsDao: TransferDao,
    private val stockDao: StockDao,
    private val inboundDao: InboundDao,
    private val itemsDao: ItemsDao,
    private val customerDao: CustomerDao
) : OutboundRepo {

    @Transaction
    override suspend fun saveFullOutbound(
        outbound: Outbound,
        details: List<OutboundDetails>,
        debtAmount: Double,
    ) {
        val finalOutboundId = if (outbound.id.isNullOrEmpty()) {
            java.util.UUID.randomUUID().toString()
        } else {
            outbound.id
        }

        val outboundEntity = outbound.toEntity().copy(id = finalOutboundId)
        outboundDao.insert(outboundEntity)

        details.forEach { detail ->
            val detailEntity = OutboundDetailesEntity(
                id = java.util.UUID.randomUUID().toString(),
                outboundId = finalOutboundId,
                itemId = detail.itemId,
                amount = detail.amount,
                price = detail.price,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
            detailsDao.insert(detailEntity)
            stockDao.reduceStock(detail.itemId, detail.amount)
        }

        customerDao.updateCustomerDebt(outbound.customerId, debtAmount)
    }

    override fun getAllOutbounds(userId: String): Flow<List<Outbound>> {
        return outboundDao.getAllOutboundWithCustomer(userId).map { entities ->
            entities.map { it.toDomain() }
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
                supabase.from("outbound_details").delete { filter { eq("outbound_id", outbound.id) } }
                supabase.from("outbound").delete { filter { eq("id", outbound.id) } }
                Log.d("SyncCheck", "Deleted from Supabase successfully")
            } catch (e: Exception) {
                Log.e("SyncCheck", "Failed to delete from remote: ${e.message}")
            }

            details.forEach { detail ->
                stockDao.updateStockQuantity(detail.itemId, detail.amount)
            }
            customerDao.decreaseCustomerBalance(outbound.customerId, totalInvoicePrice)
            detailsDao.deleteDetailsByOutboundId(outbound.id)
            outboundDao.deleteOutbound(outbound.toEntity().copy(id = outbound.id))

        } catch (e: Exception) {
            Log.e("SyncCheck", "General error during delete: ${e.message}")
            throw e
        }
    }

    override suspend fun syncEverything() {
        try {
            val currentTime = System.currentTimeMillis()

            customerDao.getUnsyncedCustomers().forEach { local ->
                handleCustomerSync(local, currentTime)
            }

            outboundDao.getAllOutbound().forEach { local ->
                handleOutboundSync(local, currentTime)
            }
            
            syncItemFromServer()

            inboundDao.getAllOutbound().forEach { local ->
                handleInboundSync(local, currentTime)
            }

            returnedDao.getUnsyncedReturns().forEach { local ->
                handleReturnedSync(local, currentTime)
            }

            transferDetailsDao.getUnsyncedTransfers().forEach { local ->
                handleTransferSync(local, currentTime)
            }

            paymentDao.getAllPayments().forEach { localPayment ->
                handlePaymentSync(localPayment, currentTime)
            }

            Log.d("Sync", "✅ المزامنة اكتملت")
        } catch (e: Exception) {
            Log.e("Sync", "خطأ عام: ${e.message}")
        }
    }

    private suspend fun handleCustomerSync(local: Customer, currentTime: Long) {
        try {
            val remote = supabase.from("customers").select {
                filter { eq("id", local.id) }
            }.decodeSingleOrNull<CustomerDto>()

            val remoteTime = remote?.updated_at ?: 0L

            when {
                remote == null || local.updatedAt!! > remoteTime -> {
                    supabase.from("customers").upsert(local.toDto().copy(updated_at = currentTime))
                    customerDao.markAsSynced(local.id)
                }
                remoteTime > local.updatedAt -> {
                    customerDao.insertCustomer(remote.toEntity())
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "خطأ في مزامنة العميل ${local.id}: ${e.message}")
        }
    }

    private suspend fun handleOutboundSync(local: OutboundEntity, currentTime: Long) {
        try {
            val remote = supabase.from("outbound").select {
                filter { eq("id", local.id) }
            }.decodeSingleOrNull<OutboundDto>()

            val remoteTime = remote?.updated_at ?: 0L

            when {
                remote == null || local.updatedAt > remoteTime -> {
                    val localDetails = detailsDao.getDetailsByOutboundIdStatic(local.id)
                    val syncParams = SaveOutboundParams(
                        p_outbound_data = local.toDto().copy(updated_at = currentTime),
                        p_details_data = localDetails.map { it.toDto() }
                    )
                    supabase.postgrest.rpc("save_outbound_with_details", syncParams)
                    outboundDao.markAsSynced(local.id)
                }
                remoteTime > local.updatedAt -> {
                    val remoteDetails = supabase.from("outbound_details")
                        .select { filter { eq("outbound_id", local.id) } }
                        .decodeList<OutboundDetailsDto>()
                    
                    outboundDao.insert(remote.toEntity().copy(isSynced = true, updatedAt = remoteTime))

                    if (remoteDetails.isNotEmpty()) {
                        detailsDao.deleteDetailsByOutboundId(local.id)
                        detailsDao.insertDetails(remoteDetails.map { it.toEntity(local.id).copy(isSynced = true, updatedAt = remoteTime) })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "❌ فشلت مزامنة الفاتورة ${local.id}: ${e.message}")
        }
    }

    private suspend fun handleInboundSync(local: InboundEntity, currentTime: Long) {
        try {
            val remote = supabase.from("inbound").select {
                filter { eq("id", local.id) }
            }.decodeSingleOrNull<InboundDto>()

            val remoteTime = remote?.updated_at ?: 0L

            when {
                remote == null || local.updatedAt > remoteTime -> {
                    val details = inboundDetailesDao.getDetailsByOutboundIdStatic(local.id)
                    val syncParams = SaveInboundParams(
                        p_inbound_data = local.toDto().copy(updated_at = currentTime),
                        p_inbound_details = details.map { it.toDto().copy(updated_at = currentTime) }
                    )
                    supabase.postgrest.rpc("save_inbound_with_details", syncParams)
                    inboundDao.markAsSynced(local.id)
                }
                remoteTime > local.updatedAt -> {
                    val remoteDetails = supabase.from("inbound_details")
                        .select { filter { eq("inbound_id", local.id) } }
                        .decodeList<InboundDetailsDto>()
                    
                    inboundDao.insert(remote.toEntity().copy(isSynced = true, updatedAt = remoteTime))
                    if (remoteDetails.isNotEmpty()){
                        inboundDetailesDao.deleteDetailsByInboundId(local.id)
                        inboundDetailesDao.insertInboundDetailsList(remoteDetails.map {
                            it.toEntity().copy(isSynced = true, updatedAt = remoteTime)
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "❌ فشلت مزامنة فاتورة الوارد ${local.id}: ${e.message}")
        }
    }

    private suspend fun handleReturnedSync(local: ReturnedEntity, currentTime: Long) {
        try {
            val remote = supabase.from("returned").select { filter { eq("id", local.id) } }.decodeSingleOrNull<ReturnedDto>()
            val remoteTime = remote?.updated_at ?: 0L

            when {
                remote == null || local.updatedAt > remoteTime -> {
                    // استخدام upsert بدلاً من delete/insert لضمان الذرية
                    supabase.from("returned").upsert(local.toDto().copy(updated_at = currentTime))
                    
                    val details = returnedDetailesDao.getDetailsByReturnId(local.id)
                    if (details.isNotEmpty()) {
                        // حذف التفاصيل القديمة في السيرفر ثم إضافة الجديدة (يفضل مستقبلاً استخدام RPC)
                        supabase.from("returned_details").delete { filter { eq("returned_id", local.id) } }
                        supabase.from("returned_details").insert(details.map { it.toDto().copy(updated_at = currentTime) })
                    }
                    returnedDao.markAsSynced(local.id)
                }
                remoteTime > local.updatedAt -> {
                    returnedDao.insert(remote.toEntity().copy(isSynced = true, updatedAt = remoteTime))
                    val remoteDetails = supabase.from("returned_details").select { filter { eq("returned_id", local.id) } }.decodeList<ReturnedDetailsDto>()
                    if (remoteDetails.isNotEmpty()){
                        returnedDetailesDao.deleteReturnedDetails(local.id)
                        returnedDetailesDao.insertReturnedDetails(remoteDetails.map { it.toEntity().copy(updatedAt = remoteTime) })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "Returned Sync Fail ${local.id}: ${e.message}")
        }
    }

    private suspend fun handleTransferSync(local: TransferEntity, currentTime: Long) {
        try {
            val remote = supabase.from("transfers").select { filter { eq("id", local.id) } }.decodeSingleOrNull<TransferDto>()
            val remoteTime = remote?.updated_at ?: 0L

            when {
                remote == null || local.updatedAt > remoteTime -> {
                    supabase.from("transfers").upsert(local.toDto().copy(updated_at = currentTime))
                    supabase.from("transfer_details").delete { filter { eq("transfer_id", local.id) } }
                    val details = transferDetailsDao.getDetailsByTransferIdSync(local.id)
                    if (details.isNotEmpty()) {
                        supabase.from("transfer_details").insert(details.map { it.toDto().copy(updated_at = currentTime) })
                    }
                }
                remoteTime > local.updatedAt -> {
                    transferDetailsDao.insertTransfer(remote.toEntity().copy(isSynced = true, updatedAt = remoteTime))
                    val remoteDetails = supabase.from("transfer_details").select { filter { eq("transfer_id", local.id) } }.decodeList<TransferDetailsDto>()
                    if (remoteDetails.isNotEmpty()){
                        transferDetailsDao.deleteTransferDetailsByParentId(local.id)
                        transferDetailsDao.insertTransferDetailList(remoteDetails.map { it.toEntity().copy(updatedAt = remoteTime) })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "Transfer Sync Fail ${local.id}: ${e.message}")
        }
    }

    private suspend fun handlePaymentSync(local: PaymentEntity, currentTime: Long) {
        try {
            val remote = supabase.from("payments").select { filter { eq("id", local.id) } }.decodeSingleOrNull<PaymentDto>()
            val remoteTime = remote?.updated_at ?: 0L

            when {
                remote == null || local.updatedAt > remoteTime -> {
                    supabase.from("payments").upsert(local.toDto().copy(updated_at = currentTime, user_id = Prefs.getUserId(context).toString()))
                    paymentDao.markAsSynced(local.id)
                }
                remoteTime > local.updatedAt -> {
                    paymentDao.insertPayment(remote.toEntity().copy(updatedAt = remoteTime))
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "Payment Sync Fail ${local.id}: ${e.message}")
        }
    }

    override suspend fun getItemsCount(): Int = outboundDao.getItemsCount()

    override suspend fun syncOutboundsFromServer(userId: String) {
        try {
            val remoteOutbounds = supabase.from("outbound").select { filter { eq("user_id", userId) } }.decodeList<OutboundDto>()
            if (remoteOutbounds.isNotEmpty()) {
                outboundDao.insertOutbounds(remoteOutbounds.map { it.toEntity().copy(isSynced = true, updatedAt = it.updated_at ?: 0L) })
                val outboundIds = remoteOutbounds.map { it.id }
                val allRemoteDetails = supabase.from("outbound_details").select { filter { isIn("outbound_id", outboundIds) } }.decodeList<OutboundDetailsDto>()
                if (allRemoteDetails.isNotEmpty()) {
                    detailsDao.insertDetails(allRemoteDetails.map { it.toEntity(it.outbound_id).copy(isSynced = true, updatedAt = it.updated_at ?: 0L) })
                }
            }
        } catch (e: Exception) {
            Log.e("SyncCheck", "Sync Error: ${e.message}")
        }
    }

    override fun getOutboundDetails(outboundId: String): Flow<List<OutboundDetailWithItemName>> = detailsDao.getDetailsListByOutboundId(outboundId)

    override suspend fun syncItemsFromServer() {
        try {
            val remoteItems = supabase.from("items").select().decodeList<Items>()
            itemsDao.insertItemsList(remoteItems.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e("SyncCheck", "Error: ${e.message}")
        }
    }

    override suspend fun syncItemFromServer() {
        try {
            val remoteItems = supabase.from("items").select().decodeList<ItemsDto>()
            val localItemsMap = itemsDao.getAllLocalItemsMetadata().associate { it.itemNum to it.updated_at }
            val itemsToUpdate = remoteItems.filter { remote -> localItemsMap[remote.itemNum.toString()] == null || (remote.updated_at ?: 0L) > (localItemsMap[remote.itemNum.toString()] ?: 0L) }
            if (itemsToUpdate.isNotEmpty()) {
                itemsDao.insertItemsList(itemsToUpdate.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Log.e("SyncCheck", "Item Sync Error: ${e.message}")
        }
    }

    @Transaction
    override suspend fun updateInvoice(outbound: Outbound, details: List<OutboundDetails>) {
        val currentTime = System.currentTimeMillis()
        val oldDetails = detailsDao.getDetailsByOutboundIdStatic(outbound.id)
        oldDetails.forEach { stockDao.updateStockQuantity(it.itemId, it.amount) }
        detailsDao.deleteDetailsByOutboundId(outbound.id)
        outboundDao.update(outbound.copy(updatedAt = currentTime, isSynced = false).toEntity())
        val detailsEntities = details.map { 
            stockDao.updateStockQuantity(it.itemId, -it.amount)
            val finalId = if (it.id.isEmpty()) java.util.UUID.randomUUID().toString() else it.id
            it.copy(id = finalId, updatedAt = currentTime).toEntity(outbound.id)
        }
        detailsDao.insertDetails(detailsEntities)
    }
}
