package com.example.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object NotesTable : Table("notes_table") {
    val id = integer("id").autoIncrement()
    val text = text("text")
    val title = text("title")
    val ownerId = integer("owner_id").references(UsersTable.id, onUpdate = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}