package com.example.myapplication.domin.model



data class Inbound (
    val id:Int,
    val invorseNum:Int,
    val userId: String,
    val fromSppliedId: Int,
    val toSppliedId: Int,
    val image: String,
    val inboundDate: String,
    val latitude: Double=0.0,
    val longitude: Double=0.0,
    val isSynced: Boolean


)