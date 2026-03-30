package com.example.myapplication.domin.repository

import com.example.myapplication.data.local.dao.ItemsDao
import com.example.myapplication.data.local.entity.OutboundEntity
import com.example.myapplication.domin.model.Customer
import com.example.myapplication.domin.model.Items
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
    suspend fun deleteInvoiceAndRollbackAll(outbound: Outbound)
    suspend fun syncOutboundsFromServer(userId: String)
}