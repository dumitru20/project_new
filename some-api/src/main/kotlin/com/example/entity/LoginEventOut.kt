package com.example.entity

import kotlinx.serialization.Serializable

@Serializable
data class LoginEventOut(val token: String?, val error: String?)
