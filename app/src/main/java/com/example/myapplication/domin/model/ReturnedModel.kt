package com.example.myapplication.domin.model

import kotlinx.datetime.LocalDate

data class ReturnedModel(

    val id: String,
    val customerId: Int,
    val returnedDate: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val updateAt: Long



)