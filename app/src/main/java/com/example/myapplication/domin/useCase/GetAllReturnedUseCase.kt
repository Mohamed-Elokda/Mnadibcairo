package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.ReturnedModel
import com.example.myapplication.domin.model.ReturnedWithNameModel
import com.example.myapplication.domin.repository.ReturnedRepo
import kotlinx.coroutines.flow.Flow

class GetAllReturnedUseCase(private val repository: ReturnedRepo) {
    fun execute(): Flow<List<ReturnedWithNameModel>> {
        return repository.getAllReturned()
    }
}