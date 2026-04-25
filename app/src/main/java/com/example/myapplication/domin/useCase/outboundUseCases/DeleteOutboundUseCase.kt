package com.example.myapplication.domin.useCase.outboundUseCases

import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.repository.OutboundRepo
import javax.inject.Inject

class DeleteOutboundUseCase @Inject constructor( private  val outboundRepo: OutboundRepo) {

    suspend  operator fun invoke(outbound: Outbound){
        outboundRepo.deleteInvoiceAndRollbackAll(outbound)
    }
}