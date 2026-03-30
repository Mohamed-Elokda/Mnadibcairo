package com.example.myapplication

import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val id: String? = null,
    val name: String,
    val business_type: String? = null,
    val logo_url: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val owner_id: String? = null,
    val created_at: String? = null
)