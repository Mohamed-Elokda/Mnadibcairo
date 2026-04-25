package com.example.myapplication.domin.model

data class Transfer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val transferNum: Int,
    val fromStoreId: String,
    val toStoreId: Int,
    val date: String,
    val userId: String,
    val isSynced: Boolean = false
)

