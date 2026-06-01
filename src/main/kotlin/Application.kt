import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.script.ScriptEngineManager

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(CallLogging)
        routing {
            post("/compile-rc") {
                val dslScript = call.receiveText()

                val realClasspath = System.getProperty("script.engine.classpath")
                    ?: System.getProperty("java.class.path")
                System.setProperty("kotlin.script.classpath", realClasspath)

                val classLoader = Thread.currentThread().contextClassLoader
                val engine = ScriptEngineManager(classLoader).getEngineByExtension("kts")
                    ?: return@post call.respondText(
                        "Kotlin script engine not found",
                        status = HttpStatusCode.InternalServerError
                    )

                val fullScript = buildString {
                    appendLine("import androidx.compose.remote.creation.*")
                    appendLine("import androidx.compose.remote.creation.dsl.*")
                    appendLine("import androidx.compose.remote.creation.modifiers.*")
                    appendLine("import androidx.compose.remote.creation.actions.*")
                    appendLine()
                    append(dslScript)
                }

                try {
                    val result = engine.eval(fullScript)
                    if (result is ByteArray) {
                        call.respondBytes(result, ContentType.Application.OctetStream)
                    } else {
                        call.respondText(
                            "Script did not return a ByteArray",
                            status = HttpStatusCode.BadRequest
                        )
                    }
                } catch (e: Exception) {
                    call.respondText(
                        e.message ?: "Compilation failed",
                        status = HttpStatusCode.BadRequest
                    )
                }
            }
        }
    }.start(wait = true)
}
