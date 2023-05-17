import com.example.entity.LoginEvent
import com.example.entity.LoginEventOut
import com.example.entity.SignUpEvent
import com.example.entity.SignUpEventOut
import com.example.module
import com.example.plugins.openDatabase
import com.example.tables.UsersTable
import com.example.tables.hash
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*

class AuthTest {

    @BeforeTest
    fun prepare() {
        openDatabase(true)
        transaction {
            UsersTable.insert {
                it[login] = "strelas"
                it[password] = hash("lkjhlkjh")
            }
        }
    }

    @Test
    fun `test login success`() {
        testApplication {
            val client = prepare()

            val eventOut = client.post("/api/login") {
                setBody(LoginEvent("strelas", "lkjhlkjh"))
            }

            assertEquals(HttpStatusCode.OK, eventOut.status)

            val event = eventOut.body<LoginEventOut>()
            assertNull(event.error)
            assertNotNull(event.token)
        }
    }

    @Test
    fun `test incorrect login`() {
        testApplication {
            val client = prepare()

            val eventOut = client.post("/api/login") {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json)
                setBody(LoginEvent("strelasss", "lkjhlkjh"))
            }

            assertEquals(HttpStatusCode.OK, eventOut.status)

            val event = eventOut.body<LoginEventOut>()
            assertEquals("incorrect login", event.error)
            assertNull(event.token)
        }
    }

    @Test
    fun `test incorrect password`() {
        testApplication {
            val client = prepare()

            val eventOut = client.post("/api/login") {

                setBody(LoginEvent("strelas", "lkjhlkjh1234"))
            }

            assertEquals(HttpStatusCode.OK, eventOut.status)

            val event = eventOut.body<LoginEventOut>()
            assertEquals("incorrect password", event.error)
            assertNull(event.token)
        }
    }

    @Test
    fun `test signUp success`() {
        testApplication {
            val client = prepare()

            val event = client.post("/api/signUp") {
                setBody(SignUpEvent("strelasss", "lkjhlkjh"))
            }

            val eventOut = event.body<SignUpEventOut>()

            assertNull(eventOut.error)
            assertEquals(HttpStatusCode.OK, event.status)

            transaction {
                assert(!UsersTable.select { (UsersTable.login eq "strelasss") and (UsersTable.password eq hash("lkjhlkjh")) }
                    .empty())
            }
        }
    }

    @Test
    fun `test signUp login exist`() {
        testApplication {
            val client = prepare()

            val event = client.post("/api/signUp") {
                setBody(SignUpEvent("strelas", "lkjhlkjh"))
            }

            assertEquals(HttpStatusCode.OK, event.status)

            val eventOut = event.body<SignUpEventOut>()
            assertEquals("login exist", eventOut.error)
        }
    }
}

fun ApplicationTestBuilder.prepare(token: String? = null): HttpClient {
    application {
        module()
    }

    return createClient {
        this.install(ContentNegotiation) {
            json()
        }

        defaultRequest {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Accept, ContentType.Application.Json)
            token?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
        }
    }
}