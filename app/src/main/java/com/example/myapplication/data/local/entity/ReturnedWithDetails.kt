package com.example.myapplication.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ReturnedWithDetails(
    @Embedded val returned: ReturnedEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "returnedId"
    )
    val details: List<ReturnedDetailsEntity>
)