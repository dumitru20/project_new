package com.example

class Config {
    companion object {
        val host: String = System.getenv("DB_HOST") ?: "localhost"
        val password: String = System.getenv("DB_PASSWORD") ?: "lkjhlkjh"
        val database: String = System.getenv("DB_NAME") ?: "notes"
    }
}