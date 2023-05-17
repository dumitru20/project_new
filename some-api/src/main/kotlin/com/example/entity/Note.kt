package com.example.entity

import kotlinx.serialization.Serializable

@Serializable
data class Note constructor(val id: Int = UNDEFINED_ID, val title: String, val text: String) {
    companion object {
        const val UNDEFINED_ID = -1
    }
}