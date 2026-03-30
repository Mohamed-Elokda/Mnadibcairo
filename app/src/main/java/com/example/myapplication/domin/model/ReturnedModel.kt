package com.example.myapplication.domin.model

import kotlinx.datetime.LocalDate

data class ReturnedModel(

    val id:Int,
    val customerId: Int,
    val itemId:Int,
    val returnedDate: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double



)