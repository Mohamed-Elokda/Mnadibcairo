package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.repository.IStockRepository
import com.example.myapplication.domin.repository.StockRepository
import kotlinx.coroutines.flow.Flow

class GetStockUseCase(private val repository: StockRepository) {
    operator fun invoke(): Flow<List<Stock>> {
        return repository.getStockData()
    }
}