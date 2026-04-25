package com.example.myapplication.domin.useCase.outboundUseCases

import com.example.myapplication.data.local.Prefs
import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.repository.OutboundRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllOutboundInvoices @Inject constructor(private val outboundRepo: OutboundRepo){

    operator fun invoke(userId: String): Flow<List<Outbound>> {
        return  outboundRepo.getAllOutbounds(userId)
    }
}