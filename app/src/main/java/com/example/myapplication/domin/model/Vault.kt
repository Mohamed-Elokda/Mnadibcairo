package com.example.myapplication.domin.model

// موجود في: domain/model/Vault.kt
data class Vault(
    val id: String,
    val amount: Double,
    val operationType: VaultOperationType, // استخدام Enum لزيادة الأمان في الكود
    val paymentMethod: PaymentMethod,
    val referenceId: String? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

enum class VaultOperationType {
    COLLECTION, DEPOSIT
}

enum class PaymentMethod {
    CASH, E_WALLET
}