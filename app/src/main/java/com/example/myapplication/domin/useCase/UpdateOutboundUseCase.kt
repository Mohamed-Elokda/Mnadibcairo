package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.model.OutboundDetails
import com.example.myapplication.domin.repository.OutboundRepo
import javax.inject.Inject

// UpdateOutboundUseCase.kt
class UpdateOutboundUseCase @Inject constructor(private val repository: OutboundRepo) {
    suspend fun execute(outbound: Outbound, details: List<OutboundDetails>) {
        repository.updateInvoice(outbound, details)
    }
}