package com.example.myapplication.domin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Items (
    val id: Int = 0,
    @SerialName("item_name") // اختيارياً: لو اسم العمود في سوبابيز مختلف (مثلاً item_name)
    val itemName: String,
    @SerialName("item_num")
    val itemNum: Int
)  {
    override fun toString(): String {
        return itemName
    }
}