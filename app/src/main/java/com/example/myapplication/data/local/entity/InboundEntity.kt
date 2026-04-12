package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Inbound")
data class InboundEntity (
    @PrimaryKey(autoGenerate = true)
    val id:Int,
    val invorseNum:Int,
    val userId: String,
    val fromSppliedId: Int,
    val image: String,
    val inboundDate: String,
    val latitude: Double=0.0,
    val longitude: Double=0.0,
    val isSynced: Boolean=false,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()


)