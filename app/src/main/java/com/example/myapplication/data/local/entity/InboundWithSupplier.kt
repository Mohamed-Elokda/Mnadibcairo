package com.example.myapplication.data.local.entity

data class InboundWithSupplier(
    // حقول الـ InboundEntity التي تهمك
    val id: String,
    val invorseNum:Int,
    val userId: String,
    val fromSppliedId: Int,
    val image: String,
    val inboundDate: String,
    val latitude: Double=0.0,
    val longitude: Double=0.0,
    val isSynced: Boolean,
    val suppliedName: String
)