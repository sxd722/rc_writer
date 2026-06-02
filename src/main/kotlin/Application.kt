import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.script.ScriptEngineManager
import javax.script.ScriptException

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
                    appendLine("import androidx.compose.remote.creation.dsl.*")
                    appendLine("import androidx.compose.remote.creation.modifiers.*")
                    appendLine("import androidx.compose.remote.creation.actions.*")
                    appendLine("import androidx.compose.remote.creation.profile.*")
                    appendLine("import androidx.compose.remote.creation.RemoteComposeWriter")
                    appendLine("import androidx.compose.remote.core.RcPlatformServices")
                    appendLine("import androidx.compose.ui.graphics.Color")
                    appendLine("import androidx.compose.ui.graphics.toArgb")
                    appendLine("import androidx.compose.ui.text.font.FontWeight")
                    appendLine()
                    appendLine("val RemoteModifier get() = Modifier")
                    appendLine("val Color.rc: Int get() = this.toArgb()")
                    appendLine()
                    appendLine("fun Modifier.clickable(name: String): Modifier = onClick { hostAction(name) }")
                    appendLine()
                    appendLine("fun Modifier.background(color: Int, cornerRadius: RcDp): Modifier =")
                    appendLine("    clip(RoundedRectShape(cornerRadius.value, cornerRadius.value, cornerRadius.value, cornerRadius.value)).background(color)")
                    appendLine()
                    appendLine("fun Modifier.padding(start: RcDp, top: RcDp, end: RcDp, bottom: RcDp): Modifier =")
                    appendLine("    padding(start.value, top.value, end.value, bottom.value)")
                    appendLine()
                    appendLine("fun Modifier.padding(horizontal: RcDp, vertical: RcDp): Modifier =")
                    appendLine("    padding(horizontal.value, vertical.value, horizontal.value, vertical.value)")
                    appendLine()
                    appendLine("fun RcScope.RemoteColumn(")
                    appendLine("    modifier: Modifier = Modifier,")
                    appendLine("    horizontalAlignment: Any? = null,")
                    appendLine("    verticalArrangement: Any? = null,")
                    appendLine("    cornerRadius: RcDp? = null,")
                    appendLine("    content: RcColumnScope.() -> Unit")
                    appendLine(") {")
                    appendLine("    val m = if (cornerRadius != null)")
                    appendLine("        modifier.clip(RoundedRectShape(cornerRadius.value, cornerRadius.value, cornerRadius.value, cornerRadius.value))")
                    appendLine("    else modifier")
                    appendLine("    val hAlign = when (horizontalAlignment?.toString()?.trim('\"')) {")
                    appendLine("        \"CenterHorizontally\", \"Center\" -> RcHorizontalPositioning.Center")
                    appendLine("        \"End\" -> RcHorizontalPositioning.End")
                    appendLine("        else -> RcHorizontalPositioning.Start")
                    appendLine("    }")
                    appendLine("    Column(modifier = m, horizontal = hAlign, content = content)")
                    appendLine("}")
                    appendLine()
                    appendLine("fun RcScope.RemoteRow(")
                    appendLine("    modifier: Modifier = Modifier,")
                    appendLine("    horizontalArrangement: RcRowHorizontalPositioning = RcRowHorizontalPositioning.Start,")
                    appendLine("    verticalAlignment: Any? = null,")
                    appendLine("    cornerRadius: RcDp? = null,")
                    appendLine("    content: RcRowScope.() -> Unit")
                    appendLine(") {")
                    appendLine("    val m = if (cornerRadius != null)")
                    appendLine("        modifier.clip(RoundedRectShape(cornerRadius.value, cornerRadius.value, cornerRadius.value, cornerRadius.value))")
                    appendLine("    else modifier")
                    appendLine("    Row(modifier = m, horizontal = horizontalArrangement, content = content)")
                    appendLine("}")
                    appendLine()
                    appendLine("fun RcScope.RemoteBox(")
                    appendLine("    modifier: Modifier = Modifier,")
                    appendLine("    contentAlignment: Any? = null,")
                    appendLine("    clickableAction: String? = null,")
                    appendLine("    cornerRadius: RcDp? = null,")
                    appendLine("    content: RcScope.() -> Unit")
                    appendLine(") {")
                    appendLine("    var m = modifier")
                    appendLine("    if (cornerRadius != null)")
                    appendLine("        m = m.clip(RoundedRectShape(cornerRadius.value, cornerRadius.value, cornerRadius.value, cornerRadius.value))")
                    appendLine("    if (clickableAction != null)")
                    appendLine("        m = m.clickable(clickableAction)")
                    appendLine("    Box(modifier = m, content = content)")
                    appendLine("}")
                    appendLine()
                    appendLine("fun RcScope.RemoteText(")
                    appendLine("    text: String,")
                    appendLine("    modifier: Modifier = Modifier,")
                    appendLine("    color: Any = 0,")
                    appendLine("    fontSize: RcSp = 0.rsp,")
                    appendLine("    fontWeight: Any? = null")
                    appendLine(") = Text(text = text, modifier = modifier, color = color, fontSize = fontSize,")
                    appendLine("    fontWeight = when (fontWeight) {")
                    appendLine("        is FontWeight -> fontWeight.weight.toFloat()")
                    appendLine("        is Number -> fontWeight.toFloat()")
                    appendLine("        else -> 0f")
                    appendLine("    })")
                    appendLine()
                    appendLine("fun RcScope.RemoteSpacer(modifier: Modifier = Modifier) = Spacer(modifier = modifier)")
                    appendLine()
                    appendLine("val __profile = RcProfile(Profile(7, 512, RcPlatformServices.None) { info, prof, cb ->")
                    appendLine("    RemoteComposeWriter(info, \"UTF-8\", prof, cb)")
                    appendLine("})")
                    appendLine("createRcBuffer(__profile) {")
                    var cleanDsl = dslScript.trim()
                    if (cleanDsl.startsWith("```")) {
                        cleanDsl = cleanDsl.substringAfter("\n").substringBeforeLast("```").trim()
                    }
                    cleanDsl = cleanDsl.replace('\u00A0', ' ').replace(Regex("[\\u2007\\u202F]"), " ")
                    appendLine(cleanDsl)
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
                } catch (e: ScriptException) {
                    var cause: Throwable = e
                    while (cause.cause != null) cause = cause.cause!!
                    val msg = buildString {
                        appendLine(cause.toString())
                        cause.stackTrace.take(8).forEach { appendLine("  at $it") }
                    }
                    call.respondText(msg, status = HttpStatusCode.BadRequest)
                } catch (e: Exception) {
                    var cause: Throwable = e
                    while (cause.cause != null) cause = cause.cause!!
                    call.respondText(
                        cause.message ?: cause.toString(),
                        status = HttpStatusCode.BadRequest
                    )
                }
            }
        }
    }.start(wait = true)
}
