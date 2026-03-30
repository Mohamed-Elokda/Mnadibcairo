package com.example.myapplication.domin.model

data class Transfer(
    val id: Int = 0,
    val transferNum: Int,
    val fromStoreId: Int,
    val toStoreId: Int,
    val date: String,
    val userId: String,
    val isSynced: Boolean = false
)

