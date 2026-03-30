package com.example.myapplication.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InboundDto(
    val id: Int,
    val user_id:String,
    val fromSupplied_id: Int,
    val toSupplied_id: Int,
    val invose_id:Int,
    val image_url:String,
    val inbound_date:String,
    val latitude: Double,
    val longitude:Double,
    val is_synced: Boolean,

    )

