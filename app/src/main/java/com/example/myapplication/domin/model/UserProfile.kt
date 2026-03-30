package com.example.myapplication.domin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("id")
    val id: String,

    @SerialName("username")
    val username: String? = null,

    @SerialName("profile")
    val profile: String? = null,

    @SerialName("password")
    val password: String? = null,


)