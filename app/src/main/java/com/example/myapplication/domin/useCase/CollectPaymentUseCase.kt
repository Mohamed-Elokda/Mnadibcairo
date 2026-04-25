package com.example.myapplication.domin.useCase


import com.example.myapplication.domin.repository.VaultRepository
import com.example.myapplication.domin.model.Vault
import com.example.myapplication.domin.model.VaultOperationType
import com.example.myapplication.domin.model.PaymentMethod
import java.util.UUID
import javax.inject.Inject

class CollectPaymentUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    suspend operator fun invoke( amount: Double, method: PaymentMethod, notes: String) {
        val vaultOperation = Vault(
            id = UUID.randomUUID().toString(),
            amount = amount,
            operationType = VaultOperationType.COLLECTION,
            paymentMethod = method,
            notes = notes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        repository.addVaultOperation(vaultOperation)
    }
}