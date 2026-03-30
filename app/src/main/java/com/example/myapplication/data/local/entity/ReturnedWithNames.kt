package com.example.myapplication.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ReturnedWithNames(
    @Embedded val returned: ReturnedEntity,


    val customerName: String?,


    val itemName: String?
)