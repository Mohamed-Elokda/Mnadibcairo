package com.example.myapplication.domin.useCase

import com.example.myapplication.data.local.entity.ReturnedDetailsEntity
import com.example.myapplication.data.local.entity.ReturnedEntity
import com.example.myapplication.domin.model.ReturnedDetailsModel
import com.example.myapplication.domin.model.ReturnedModel
import com.example.myapplication.domin.repository.ReturnedRepo

class AddReturnedUseCase(private val repository: ReturnedRepo) {
    suspend fun saveLocally(returned: ReturnedModel, details: List<ReturnedDetailsModel>, debtAmount: Double): Result<Unit> {
        return try {
            repository.insertReturned(returned, details,debtAmount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // تنفيذ عملية المزامنة مع السيرفر
    suspend fun syncWithServer(): Result<Unit> {
        return try {
            repository.syncReturnsWithServer()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}