package com.example.myapplication.domin.repository

import com.example.myapplication.domin.model.ItemMovement
import com.example.myapplication.domin.model.Stock
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    fun getItemMovement(itemId: Int): Flow<List<ItemMovement>>
    fun getStockData(): Flow<List<Stock>>
    suspend fun syncStockFromServer(userId: String)
    suspend fun reconcileAllStocks()
}