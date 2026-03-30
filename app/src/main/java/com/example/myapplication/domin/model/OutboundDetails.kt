package com.example.myapplication.domin.model

data class OutboundDetails(

    val id: Long = 0,
    val outboundId: Long,
    val itemId: Int,
    val amount: Int,
    val price: Double,
    val isSynced: Boolean


)
