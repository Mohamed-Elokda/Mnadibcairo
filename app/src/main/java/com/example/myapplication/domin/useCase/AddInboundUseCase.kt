package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.Inbound
import com.example.myapplication.domin.model.InboundDetails
import com.example.myapplication.domin.repository.IInboundRepository

// AddInboundUseCase.kt
class AddInboundUseCase(private val repository: IInboundRepository) {
    suspend operator fun invoke(inbound: Inbound, details: List<InboundDetails>): Result<Unit> {
        // التحقق من الأصناف
        for (item in details) {
            val exists = repository.checkItemExists(item.ItemId)
            if (!exists) return Result.failure(Exception("الصنف رقم ${item.ItemId} غير موجود!"))
        }

        // ملاحظة هامة: إذا كان الـ id أكبر من 0، فهذا يعني أنه "تعديل"
        // سنعتمد على الـ Repository للقيام بمسح القديم وإضافة الجديد
        return repository.saveInboundTransaction(inbound, details)
    }
}