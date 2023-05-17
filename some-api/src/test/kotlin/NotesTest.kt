import com.example.entity.LoginEvent
import com.example.entity.LoginEventOut
import com.example.entity.Note
import com.example.plugins.openDatabase
import com.example.tables.NotesTable
import com.example.tables.UsersTable
import com.example.tables.hash
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class NotesTest {
    private var noteId1 = -1
    private var noteId2 = -1
    private var token = ""

    @BeforeTest
    fun prepare() {
        openDatabase(true)
        transaction {
            val userId = UsersTable.insert {
                it[login] = "strelas"
                it[password] = hash("lkjhlkjh")
            }[UsersTable.id]

            noteId1 = NotesTable.insert {
                it[ownerId] = userId
                it[text] = "text 1"
                it[title] = "title 1"
            }[NotesTable.id]

            noteId2 = NotesTable.insert {
                it[ownerId] = userId
                it[text] = "text 2"
                it[title] = "title 2"
            }[NotesTable.id]
        }

        testApplication {
            val client = prepare()

            val event = client.post("/api/login") {
                setBody(LoginEvent("strelas", "lkjhlkjh"))
            }


            val eventOut = event.body<LoginEventOut>()
            token = eventOut.token ?: token
        }
    }

    @Test
    fun `test get notes`() {
        testApplication {
            val client = prepare(token)

            val event = client.get("/api/notes")

            assertEquals(HttpStatusCode.OK, event.status)
            val notes = event.body<List<Note>>()
            assertEquals(2, notes.size)
            val ids = notes.map { it.id }
            assert(ids.contains(noteId1))
            assert(ids.contains(noteId2))
        }
    }

    @Test
    fun `test get notes unauthorized`() {
        testApplication {
            val client = prepare()

            val event = client.get("/api/notes")

            assertEquals(HttpStatusCode.Unauthorized, event.status)
        }
    }

    @Test
    fun `test get single note`() {
        testApplication {
            val client = prepare(token)

            val event = client.get("/api/note/$noteId1")

            assertEquals(HttpStatusCode.OK, event.status)

            val note = event.body<Note>()

            assertEquals(noteId1, note.id)
            assertEquals("text 1", note.text)
            assertEquals("title 1", note.title)
        }
    }

    @Test
    fun `test get single note unauthorized`() {
        testApplication {
            val client = prepare()

            val event = client.get("/api/note/$noteId1")

            assertEquals(HttpStatusCode.Unauthorized, event.status)
        }
    }

    @Test
    fun `test delete note`() {
        testApplication {
            val client = prepare(token)

            val event = client.delete("/api/note/$noteId1")

            assertEquals(HttpStatusCode.OK, event.status)

            transaction {
                assert(NotesTable.select { NotesTable.id eq noteId1 }.empty())
            }
        }
    }

    @Test
    fun `test delete non exist note`() {
        testApplication {
            val client = prepare(token)

            val event = client.delete("/api/note/${noteId1 + 100}")

            assertEquals(HttpStatusCode.NotFound, event.status)
        }
    }

    @Test
    fun `test delete note unauthorized`() {
        testApplication {
            val client = prepare()

            val event = client.delete("/api/note/$noteId1")

            assertEquals(HttpStatusCode.Unauthorized, event.status)
        }
    }

    @Test
    fun `test create new note`() {
        testApplication {
            val client = prepare(token)

            val event = client.post("/api/note") {
                setBody(Note(Note.UNDEFINED_ID, "new title", "new text"))
            }

            assertEquals(HttpStatusCode.OK, event.status)

            transaction {
                assert(!NotesTable.select { NotesTable.title eq "new title" }.empty())
            }
        }
    }

    @Test
    fun `test edit note`() {
        testApplication {
            val client = prepare(token)

            val event = client.post("/api/note") {
                setBody(Note(noteId1, "new title", "new text"))
            }

            assertEquals(HttpStatusCode.OK, event.status)

            transaction {
                val row = NotesTable.select { NotesTable.id eq noteId1 }.apply { assert(!empty()) }.first()
                assertEquals("new title", row[NotesTable.title])
                assertEquals("new text", row[NotesTable.text])
            }
        }
    }

    @Test
    fun `test not found note`() {
        testApplication {
            val client = prepare(token)

            val event = client.post("/api/note") {
                setBody(Note(noteId1 + 100, "new title", "new text"))
            }

            assertEquals(HttpStatusCode.NotFound, event.status)
        }
    }

    @Test
    fun `test create note unauthorized`() {
        testApplication {
            val client = prepare()

            val event = client.post("/api/note") {
                setBody(Note(Note.UNDEFINED_ID, "new title", "new text"))
            }

            assertEquals(HttpStatusCode.Unauthorized, event.status)
        }

    }
}