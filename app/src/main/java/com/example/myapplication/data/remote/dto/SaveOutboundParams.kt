package com.example.myapplication.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SaveOutboundParams(
    val p_outbound_data: OutboundDto,
    val p_details_data: List<OutboundDetailsDto>
)