package com.example.myapplication.domin.repository

import com.example.myapplication.domin.model.Stock
import kotlinx.coroutines.flow.Flow

interface IStockRepository {
    fun getStockData(): Flow<List<Stock>>
}