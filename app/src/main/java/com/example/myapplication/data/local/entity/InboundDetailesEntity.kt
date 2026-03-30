package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("InboundDetailes")
data class InboundDetailesEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val InboundId:Int,
    val ItemId: Int,
    val amount:Int,
    val isSynced: Boolean=false
)