package com.example.tables

import org.jetbrains.exposed.sql.Table
import java.math.BigInteger
import java.security.MessageDigest

object UsersTable : Table("users_table") {
    val id = integer("id").autoIncrement()
    val login = text("login")
    val password = text("password")
    override val primaryKey = PrimaryKey(id)
}

fun hash(text: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(text.toByteArray())).toString(16).padStart(32, '0')
}