package com.example.myapplication.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemsDto(
    @SerialName("id")
    val id: Int? = null, // المعرف التلقائي في السيرفر

    @SerialName("item_num")
    val itemNum: Long, // الكود الذي سنستخدمه للمقارنة (مثل الباركود)

    @SerialName("item_name")
    val itemName: String,



    @SerialName("updated_at")
    val updated_at: Long? = null // ضروري جداً للمزامنة الزمنية
)