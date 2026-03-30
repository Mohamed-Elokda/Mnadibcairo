package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.model.OutboundDetails
import com.example.myapplication.domin.model.Resource
import com.example.myapplication.domin.repository.OutboundRepo

// ProcessOutboundUseCase.kt
class ProcessOutboundUseCase(private val outboundRepo: OutboundRepo) {
    suspend operator fun invoke(
        outbound: Outbound,
        details: List<OutboundDetails>,

    ): Resource<Long> { // تغيير نوع الإرجاع هنا
        return try {
            val totalInvoice = details.sumOf { it.amount * it.price }
            val remainingDebt = totalInvoice - outbound.moneyResive

            outboundRepo.saveFullOutbound(outbound, details, remainingDebt)

            // نعيد النجاح مع قيمة Long (مثلاً 1 أو معرف العملية)
            Resource.Success(1L)
        } catch (e: Exception) {
            // نعيد خطأ مع الرسالة
            Resource.Error(e.message ?: "حدث خطأ غير متوقع")
        }
    }
}
