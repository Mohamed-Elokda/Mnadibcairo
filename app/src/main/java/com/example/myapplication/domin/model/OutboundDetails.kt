package com.example.myapplication.domin.model

data class OutboundDetails(

    val id: String ,
    val outboundId: Long,
    val itemId: Int,
    var amount: Int,
    var price: Double,
    val isSynced: Boolean,
    val updatedAt: Long



)
