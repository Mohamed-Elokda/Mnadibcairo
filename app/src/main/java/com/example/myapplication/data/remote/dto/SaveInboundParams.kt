package com.example.myapplication.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SaveInboundParams(
    val p_inbound_data: InboundDto,
    val p_inbound_details: List<InboundDetailsDto>
)