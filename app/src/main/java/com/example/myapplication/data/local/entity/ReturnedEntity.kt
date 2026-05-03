package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(tableName = "returned")
data class ReturnedEntity(

    @PrimaryKey()
    val id: String,
    val customerId: Int,
    val returnedDate: String,
    val userId: String,
    val invoiceNum: String,
    val latitude: Double,
    val longitude: Double,
    val isSynced: Boolean,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()


)
