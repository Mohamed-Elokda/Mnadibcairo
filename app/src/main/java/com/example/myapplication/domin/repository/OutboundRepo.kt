package com.example.myapplication.domin.repository

import com.example.myapplication.data.local.entity.OutboundDetailWithItemName
import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.model.OutboundDetails
import com.example.myapplication.domin.model.Stock
import kotlinx.coroutines.flow.Flow

// OutboundRepo.kt
interface OutboundRepo {
    suspend fun saveFullOutbound(
        outbound: Outbound,
        details: List<OutboundDetails>,
        debtAmount: Double
    )
    fun getAllItems(): Flow<List<Stock>>
    suspend fun checkItemExists(itemId: Int): Boolean
    fun getAllOutbounds(userId: String): Flow<List<Outbound>>
    suspend fun getItemsCount(): Int
    suspend fun syncItemsFromServer()
    suspend fun syncEverything()
    suspend fun deleteInvoiceAndRollbackAll(outbound: Outbound)
    suspend fun syncOutboundsFromServer(userId: String)
     fun getOutboundDetails(outboundId: String): Flow<List<OutboundDetailWithItemName>>


    suspend fun updateInvoice(outbound: Outbound, details: List<OutboundDetails>)
    suspend fun syncItemFromServer()
}