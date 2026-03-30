package com.example.myapplication.domin.repository

import com.example.myapplication.data.local.entity.ItemHistoryProjection
import com.example.myapplication.data.local.entity.ItemsEntity
import com.example.myapplication.domin.model.ReturnedDetailsModel
import com.example.myapplication.domin.model.ReturnedModel
import com.example.myapplication.domin.model.ReturnedWithNameModel
import kotlinx.coroutines.flow.Flow

interface ReturnedRepo {

    suspend fun insertReturned(returnedModel: ReturnedModel,returnedDetails:List<ReturnedDetailsModel>, debtAmount: Double
    )

     fun getAllReturned(): Flow<List<ReturnedWithNameModel>>

    suspend fun getAllReturnedDetailsByReturnedId(returnedId:Int): Flow<List<ReturnedDetailsModel>>

    fun getItemsByCustomer(customerId: Int): Flow<List<ItemsEntity>>

    // جلب سعر آخر بيع لصنف معين لعميل معين
    suspend fun getLastPrice(customerId: Int, itemId: Int): Double?

    fun getAllReturnedDetails(returnedId: Int): Flow<List<ReturnedDetailsModel>>
    fun getItemPurchaseHistory(customerId: Int, itemId: Int): Flow<List<ItemHistoryProjection>>

    suspend fun syncReturnsWithServer()
    suspend fun syncReturnsFromServer(userId: String)
}