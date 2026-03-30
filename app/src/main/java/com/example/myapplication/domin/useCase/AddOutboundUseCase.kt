package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.model.OutboundDetails
import com.example.myapplication.domin.repository.OutboundRepo

class AddOutboundUseCase(private val repository: OutboundRepo) {
    suspend operator fun invoke(
        outbound: Outbound,
        details: List<OutboundDetails>,
        debtAmount: Double,
        lat: Double?,
        lon: Double?
    ): Result<Long> { // استخدام Result لتسهيل معالجة الأخطاء في ViewModel
        return try {
            for (item in details) {
                val exists = repository.checkItemExists(item.itemId.toInt())
                if (!exists) {
                    return Result.failure(Exception("الصنف رقم ${item.itemId} غير موجود!"))
                }
            }
            repository.saveFullOutbound(outbound, details, debtAmount)
            Result.success(1L)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}