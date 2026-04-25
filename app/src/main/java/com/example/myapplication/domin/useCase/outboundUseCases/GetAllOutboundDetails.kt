package com.example.myapplication.domin.useCase.outboundUseCases

import com.example.myapplication.data.local.entity.OutboundDetailWithItemName
import com.example.myapplication.data.local.entity.OutboundWithDetails
import com.example.myapplication.domin.repository.OutboundRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllOutboundDetails @Inject constructor(private val outboundRepo: OutboundRepo ) {

    suspend operator fun invoke(outboundId: String): Flow<List<OutboundDetailWithItemName>> {
        return outboundRepo.getOutboundDetails(outboundId)
    }
}