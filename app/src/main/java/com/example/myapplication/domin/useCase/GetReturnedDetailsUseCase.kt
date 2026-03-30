package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.ReturnedDetailsModel
import com.example.myapplication.domin.repository.ReturnedRepo
import kotlinx.coroutines.flow.Flow

class GetReturnedDetailsUseCase(private val repository: ReturnedRepo) {
    operator fun invoke(returnedId: Int): Flow<List<ReturnedDetailsModel>> {
        return repository.getAllReturnedDetails(returnedId)
    }
}