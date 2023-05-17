package com.example.entity

import kotlinx.serialization.Serializable

@Serializable
data class SignUpEvent(val login: String, val password: String)
