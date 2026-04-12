package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.repository.StockRepository
import kotlinx.coroutines.flow.Flow

class GetStockFromServer (private val repository: StockRepository) {
   suspend  operator fun invoke(userId: String) {
         repository.syncStockFromServer(userId)
    }
}