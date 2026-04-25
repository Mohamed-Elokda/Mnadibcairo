package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.ReturnedDetailsModel
import com.example.myapplication.domin.repository.ReturnedRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class  GetReturnedDetailsUseCase @Inject constructor(private val repository: ReturnedRepo) {
    operator fun invoke(returnedId: String): Flow<List<ReturnedDetailsModel>> {
        return repository.getAllReturnedDetails(returnedId)
    }
}