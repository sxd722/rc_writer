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
                    appendLine("import androidx.compose.remote.creation.profile.*")
                    appendLine("import androidx.compose.remote.core.RcPlatformServices")
                    appendLine("import androidx.compose.ui.graphics.Color")
                    appendLine("import androidx.compose.ui.text.font.FontWeight")
                    appendLine()
                    appendLine("val RemoteModifier get() = Modifier")
                    appendLine("val Color.rc: Int get() = (red * 255f).toInt().let { r -> (green * 255f).toInt().let { g -> (blue * 255f).toInt().let { b -> (alpha * 255f).toInt().shl(24).or(r.shl(16)).or(g.shl(8)).or(b) } } }")
                    appendLine("class RemoteRoundedCornerShape(val r: RcDp)")
                    appendLine("object RemoteArrangement { val SpaceEvenly = RcRowHorizontalPositioning.SpaceEvenly; val SpaceBetween = RcRowHorizontalPositioning.SpaceBetween; val SpaceAround = RcRowHorizontalPositioning.SpaceAround; val Start = RcRowHorizontalPositioning.Start; val End = RcRowHorizontalPositioning.End }")
                    appendLine("object RemoteAlignment { val Center = 0; val Start = 1; val End = 2; val Top = 3; val Bottom = 4 }")
                    appendLine()
                    appendLine("fun Modifier.clickable(name: String): Modifier = onClick { hostAction(name) }")
                    appendLine("fun Modifier.padding(horizontal: RcDp, vertical: RcDp): Modifier = padding(horizontal.value, vertical.value, horizontal.value, vertical.value)")
                    appendLine("fun Modifier.padding(vertical: RcDp): Modifier = padding(0f, vertical.value, 0f, vertical.value)")
                    appendLine("fun Modifier.background(color: Int, shape: RemoteRoundedCornerShape?): Modifier { var m: Modifier = this; if (shape != null) m = m.clip(RoundedRectShape(shape.r.value, shape.r.value, shape.r.value, shape.r.value)); return m.background(color) }")
                    appendLine("fun Modifier.border(width: RcDp, color: Int, shape: RemoteRoundedCornerShape?): Modifier = border(width.value, 0f, color, if (shape != null) 2 else 0)")
                    appendLine()
                    appendLine("fun RcScope.RemoteColumn(modifier: Modifier = Modifier, content: RcColumnScope.() -> Unit) = Column(modifier = modifier, content = content)")
                    appendLine("fun RcScope.RemoteRow(modifier: Modifier = Modifier, horizontalArrangement: RcRowHorizontalPositioning = RcRowHorizontalPositioning.Start, content: RcRowScope.() -> Unit) = Row(modifier = modifier, horizontal = horizontalArrangement, content = content)")
                    appendLine("fun RcScope.RemoteBox(modifier: Modifier = Modifier, contentAlignment: Any? = null, content: RcScope.() -> Unit) = Box(modifier = modifier, horizontal = RcHorizontalPositioning.Center, vertical = RcVerticalPositioning.Center, content = content)")
                    appendLine("fun RcScope.RemoteText(text: String, modifier: Modifier = Modifier, color: Any = 0, fontSize: RcSp = 0.rsp, fontWeight: Any? = null) = Text(text = text, modifier = modifier, color = color, fontSize = fontSize, fontWeight = when(fontWeight) { is FontWeight -> fontWeight.weight.toFloat(); is Number -> fontWeight.toFloat(); else -> 0f })")
                    appendLine("fun RcScope.RemoteSpacer(modifier: Modifier = Modifier) = Spacer(modifier = modifier)")
                    appendLine()
                    appendLine("val __profile = RcProfile(Profile(7, 512, RcPlatformServices.None) { info, prof, cb ->")
                    appendLine("    RemoteComposeWriter(info, \"UTF-8\", prof, cb)")
                    appendLine("})")
                    appendLine("createRcBuffer(__profile) {")
                    appendLine(dslScript)
                    appendLine("}")
                }

                println("--- COMPILING SCRIPT ---")
                println(fullScript)
                println("------------------------")

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
