package com.example.entity

import kotlinx.serialization.Serializable

@Serializable
data class LoginEvent(val login: String, val password: String)