package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.Transfer
import com.example.myapplication.domin.model.TransferDetails
import com.example.myapplication.domin.repository.ITransferRepository

// AddTransferUseCase.kt
class AddTransferUseCase(private val repository: ITransferRepository) {
    suspend operator fun invoke(transfer: Transfer, details: List<TransferDetails>): Result<Unit> {
        // التحقق من أن المخزن المصدر موجود (مخزن المستخدم)
        if (transfer.fromStoreId <= 0) {
            return Result.failure(Exception("لم يتم تحديد مخزن المصدر الخاص بك"))
        }

        // منع النقل لنفس المخزن
        if (transfer.fromStoreId == transfer.toStoreId) {
            return Result.failure(Exception("لا يمكن النقل لمخزنك الخاص، اختر مخزناً آخر"))
        }

        if (details.isEmpty()) {
            return Result.failure(Exception("قائمة الأصناف فارغة"))
        }

        return repository.saveTransfer(transfer, details)
    }
}