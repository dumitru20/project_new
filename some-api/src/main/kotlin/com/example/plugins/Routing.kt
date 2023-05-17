package com.example.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.entity.*
import com.example.tables.NotesTable
import com.example.tables.UsersTable
import com.example.tables.hash
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Application.configureRouting() {
    fun generateToken(login: String, id: Int): String =
        JWT.create()
            .withAudience("http://0.0.0.0:8080/")
            .withIssuer("http://0.0.0.0:8080/")
            .withClaim("username", login)
            .withClaim("id", id)
            .withExpiresAt(Date(System.currentTimeMillis() + 60000 * 60 * 24))
            .sign(Algorithm.HMAC256("secret"))


    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Access to 'hello'"
            verifier(
                JWT
                    .require(Algorithm.HMAC256("secret"))
                    .withAudience("http://0.0.0.0:8080/")
                    .withIssuer("http://0.0.0.0:8080/")
                    .build()
            )

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }

            validate { credential ->
                if (credential.payload.getClaim("username").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
    routing {
        post("/api/login") {
            val event = call.receive<LoginEvent>()
            val passwordHash = transaction {
                UsersTable.select { UsersTable.login eq event.login }
                    .map { Pair(it[UsersTable.password], it[UsersTable.id]) }.firstOrNull()
            }

            if (passwordHash == null) {
                call.respond(LoginEventOut(null, "incorrect login"))
                return@post
            }

            if (passwordHash.first != hash(event.password)) {
                call.respond(LoginEventOut(null, "incorrect password"))
                return@post
            }

            call.respond(LoginEventOut(generateToken(event.login, passwordHash.second), null))
        }

        post("/api/signUp") {
            val event = call.receive<SignUpEvent>()
            val sameLogin = transaction {
                UsersTable.select { UsersTable.login eq event.login }.firstOrNull()
            }

            if (sameLogin != null) {
                call.respond(SignUpEventOut("login exist"))
                return@post
            }

            transaction {
                UsersTable.insert {
                    it[login] = event.login
                    it[password] = hash(event.password)
                }
            }

            call.respond(SignUpEventOut(null))
        }

        authenticate("auth-jwt") {
            get("/api/notes") {
                val userId = call.getUserId()

                val notes = transaction {
                    NotesTable.select { NotesTable.ownerId eq userId }.map {
                        Note(it[NotesTable.id], it[NotesTable.title], it[NotesTable.text])
                    }
                }

                call.respond(notes)
            }

            delete("/api/note/{id}") {
                val userId = call.getUserId()
                val noteId = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("incorrect note id")

                val deleted = transaction {
                    NotesTable.deleteWhere { (id eq noteId) and (ownerId eq userId) }
                }

                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }

                call.respond(HttpStatusCode.OK)
            }

            get("/api/note/{id}") {
                val userId = call.getUserId()
                val noteId = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("incorrect note id")

                val note = transaction {
                    NotesTable.select { (NotesTable.ownerId eq userId) and (NotesTable.id eq noteId) }.map {
                        Note(it[NotesTable.id], it[NotesTable.title], it[NotesTable.text])
                    }.firstOrNull()
                }

                if (note == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                call.respond(note)
            }

            post("/api/note") {
                val userId = call.getUserId()
                val note = call.receive<Note>()
                val hasError = transaction {
                    if (note.id != Note.UNDEFINED_ID) {
                        val dbNote = NotesTable.select { (NotesTable.id eq note.id) and (NotesTable.ownerId eq userId) }
                            .firstOrNull()
                        if (dbNote == null) {
                            true
                        } else {
                            NotesTable.update({ NotesTable.id eq note.id }) {
                                it[text] = note.text
                                it[title] = note.title
                            }
                            false
                        }
                    } else {
                        NotesTable.insert {
                            it[text] = note.text
                            it[title] = note.title
                            it[ownerId] = userId
                        }
                        false
                    }
                }

                if (hasError) {
                    call.respond(HttpStatusCode.NotFound)
                }
                call.respond(call.respond(HttpStatusCode.OK))
            }
        }
    }
}

fun ApplicationCall.getUserId(): Int {
    return principal<JWTPrincipal>()!!.payload.getClaim("id").asInt()
}
