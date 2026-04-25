package com.example.myapplication.domin.model

data class TransferWithStoreName(
    val id: String,
    val transferNum: Int,
    val fromStoreId: String,
    val toStoreId: Int,
    val toStoreName: String, // ده الحقل الجديد اللي هنجيبه من جدول Supplied
    val date: String,
    val userId: String,
    val isSynced: Boolean
)