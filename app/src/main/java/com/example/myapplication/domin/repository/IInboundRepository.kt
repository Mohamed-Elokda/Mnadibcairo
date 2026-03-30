package com.example.myapplication.domin.repository

import com.example.myapplication.data.local.entity.InboundDetailWithItemName
import com.example.myapplication.data.local.entity.InboundDetailesEntity
import com.example.myapplication.data.local.entity.InboundEntity
import com.example.myapplication.data.local.entity.ItemsEntity
import com.example.myapplication.domin.model.Items
import com.example.myapplication.data.local.entity.StockEntity
import com.example.myapplication.data.local.entity.Supplied
import com.example.myapplication.domin.model.Inbound
import com.example.myapplication.domin.model.InboundDetails
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.model.SuppliedModel
import kotlinx.coroutines.flow.Flow

interface IInboundRepository {
    // عرض الفواتير والمخزن
    fun getAllInbounds(userId: String): Flow<List<Inbound>>
    fun getStock(): Flow<List<Stock>>

    // عمليات الحفظ والتحقق
    suspend fun checkItemExists(itemId: Int): Boolean
    suspend fun saveInboundTransaction(inbound: Inbound, details: List<InboundDetails>): Result<Unit>
    fun getAllItems(): Flow<List<Items>>
    suspend fun insertItemsList(entities: List<ItemsEntity>)
    fun getDetailsByInboundId(inboundId: Long): kotlinx.coroutines.flow.Flow<kotlin.collections.List<com.example.myapplication.data.local.entity.InboundDetailWithItemName>>
    suspend fun addInSupbase(inbound: Inbound, details: List<InboundDetails>)
    suspend fun deleteInbound(inbound: Inbound)
    suspend fun deleteFullInbound(inbound: Inbound): Result<Unit>
    suspend fun syncInboundFromServer(userId: String)
    fun getAllSupplied(): Flow<List<SuppliedModel>>
}