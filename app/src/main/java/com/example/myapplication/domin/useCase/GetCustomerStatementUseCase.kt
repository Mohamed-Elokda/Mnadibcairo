package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.StatementTransaction
import com.example.myapplication.domin.repository.StatementRepository
import kotlinx.coroutines.flow.Flow

class GetCustomerStatementUseCase(private val repository: StatementRepository) {
    operator fun invoke(customerId: Int): Flow<List<StatementTransaction>> {
        return repository.getCustomerStatement(customerId)
    }
}