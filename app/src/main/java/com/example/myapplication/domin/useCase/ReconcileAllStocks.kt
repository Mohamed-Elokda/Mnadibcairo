package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.repository.StockRepository
import javax.inject.Inject

class ReconcileAllStocks @Inject constructor(private val stockRepository: StockRepository) {
    suspend operator fun invoke(){
        stockRepository.reconcileAllStocks()
    }
}