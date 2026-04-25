package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.Transfer
import com.example.myapplication.domin.model.TransferDetails
import com.example.myapplication.domin.repository.ITransferRepository
import javax.inject.Inject

// AddTransferUseCase.kt
class AddTransferUseCase @Inject constructor(private val repository: ITransferRepository) {
    suspend operator fun invoke(transfer: Transfer, details: List<TransferDetails>): Result<Unit> {

        if (details.isEmpty()) {
            return Result.failure(Exception("قائمة الأصناف فارغة"))
        }

        return repository.saveTransfer(transfer, details)
    }
}