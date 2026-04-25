package com.example.myapplication.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class OutboundWithDetails(
    @Embedded val outbound: OutboundEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "outboundId"
    )
       val details: List<OutboundDetailesEntity>

)