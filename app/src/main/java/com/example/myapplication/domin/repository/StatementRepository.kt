package com.example.myapplication.domin.repository

import com.example.myapplication.domin.model.StatementTransaction
import kotlinx.coroutines.flow.Flow

interface StatementRepository {
    fun getCustomerStatement(customerId: Int): Flow<List<StatementTransaction>>
    suspend fun reconcileAllCustomersDebt()
}