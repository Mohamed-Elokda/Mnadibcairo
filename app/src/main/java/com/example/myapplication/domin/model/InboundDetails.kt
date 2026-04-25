package com.example.myapplication.domin.model



data class InboundDetails (

    val id: String,
    val InboundId: String,
    val ItemId: Int,
    var amount:Int,
    val userId: String,
    val updated_at: Long

) {
}