package com.example.plugins

import com.example.Config
import com.example.tables.NotesTable
import com.example.tables.UsersTable
import org.jetbrains.exposed.sql.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    openDatabase()
}

fun openDatabase(testing: Boolean = false) {
    val url = "jdbc:mysql://${
        Config.host
    }/${Config.database}?characterEncoding=utf8&allowPublicKeyRetrieval=true&useSSL=false"
    val user = "root"
    val password = Config.password

    Database.connect(
        url,
        driver = "com.mysql.cj.jdbc.Driver",
        user = user,
        password = password
    )

    transaction {
        if (testing) {
            SchemaUtils.drop(*sqlTables)
        }
        SchemaUtils.create(*sqlTables)
        SchemaUtils.addMissingColumnsStatements(*sqlTables)
    }
}

val sqlTables = arrayOf(UsersTable, NotesTable)
