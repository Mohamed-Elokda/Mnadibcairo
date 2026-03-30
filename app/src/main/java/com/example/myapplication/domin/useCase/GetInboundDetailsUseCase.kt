package com.example.myapplication.domin.useCase

import com.example.myapplication.data.local.entity.InboundDetailWithItemName
import com.example.myapplication.domin.repository.IInboundRepository
import kotlinx.coroutines.flow.Flow

class GetInboundDetailsUseCase(private val repository: IInboundRepository) {

    // استخدام operator invoke يجعل استدعاء الـ UseCase سهلاً كأنه دالة
    operator fun invoke(inboundId: Long): Flow<List<InboundDetailWithItemName>> {
        return repository.getDetailsByInboundId(inboundId)
    }
}