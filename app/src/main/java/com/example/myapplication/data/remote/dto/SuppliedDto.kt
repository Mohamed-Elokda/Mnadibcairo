package com.example.myapplication.data.remote.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SuppliedDto(
val id:Int,
@SerialName("supplied_name") // هذا يربط اسم العمود في سوبابيس بالحقل في كوتلن
val supplied_name:String,
val num: String,
val updated_at: Long? = null // ستقوم Supabase بتعبئته تلقائياً عند الجلب

)
