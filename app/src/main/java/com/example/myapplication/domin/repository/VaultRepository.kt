package com.example.myapplication.domin.repository

import com.example.myapplication.data.local.entity.VaultEntity
import com.example.myapplication.domin.model.Vault

interface VaultRepository {
    suspend fun collectPayment(operation: VaultEntity): Result<Unit>
    suspend fun depositToCompany(operation: VaultEntity): Result<Unit>
    suspend fun getBalance(userId: String): Double
    suspend fun getHistory(userId: String): List<VaultEntity>
    suspend fun syncWithTimestampStrategy(): Result<Unit>
    suspend fun addVaultOperation(vaultOperation: Vault)
}