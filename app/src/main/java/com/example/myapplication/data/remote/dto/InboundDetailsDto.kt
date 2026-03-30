package com.example.myapplication.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InboundDetailsDto (
    val id: Int,
    val inbound_id:Int,
    val item_id:Int,
    val amount:Int,




    ){
}