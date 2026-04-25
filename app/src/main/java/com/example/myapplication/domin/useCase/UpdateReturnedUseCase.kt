package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.ReturnedDetailsModel
import com.example.myapplication.domin.model.ReturnedModel
import com.example.myapplication.domin.repository.ReturnedRepo
import jakarta.inject.Inject

class UpdateReturnedUseCase @Inject constructor(
    private val repository: ReturnedRepo
) {
    suspend operator fun invoke(
        returned: ReturnedModel,
        details: List<ReturnedDetailsModel>,
        newDebtAmount: Double
    ): Result<Unit> {
        return try {
            repository.updateReturned(returned, details, newDebtAmount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}