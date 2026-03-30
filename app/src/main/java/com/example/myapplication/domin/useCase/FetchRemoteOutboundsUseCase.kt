package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.repository.OutboundRepo

class FetchRemoteOutboundsUseCase(
    private val repository: OutboundRepo
) {
    suspend operator fun invoke(userId: String): Result<Unit> {
        return try {
            // جلب البيانات من السيرفر وحفظها محلياً
            repository.syncOutboundsFromServer(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}