package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStockFromServer @Inject constructor(private val repository: StockRepository) {
   suspend  operator fun invoke(userId: String) {
         repository.syncStockFromServer(userId)
    }
}