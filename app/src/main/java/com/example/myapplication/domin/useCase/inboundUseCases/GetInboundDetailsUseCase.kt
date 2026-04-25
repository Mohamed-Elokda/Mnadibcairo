package com.example.myapplication.domin.useCase.inboundUseCases

import com.example.myapplication.data.local.entity.InboundDetailWithItemName
import com.example.myapplication.domin.repository.IInboundRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInboundDetailsUseCase @Inject constructor(private val repository: IInboundRepository) {

    // استخدام operator invoke يجعل استدعاء الـ UseCase سهلاً كأنه دالة
    operator fun invoke(inboundId: String): Flow<List<InboundDetailWithItemName>> {
        return repository.getDetailsByInboundId(inboundId)
    }
}