# RC Writer

A Ktor-based server that compiles Kotlin DSL scripts into RemoteCompose (RC) binary documents using the `androidx.compose.remote` library.

## Prerequisites

- **JDK 21** (Temurin recommended) — set `JAVA_HOME` accordingly
- **Gradle** (wrapper included)

## Build and Run

```bash
# Set JAVA_HOME (adjust path to your JDK 21 install)
# Windows:
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
# macOS/Linux:
export JAVA_HOME=/path/to/jdk-21

# Build and start the server
./gradlew run
```

The server starts on `http://0.0.0.0:8080`.

## Usage

Send a POST request with a Kotlin DSL snippet to `/compile-rc`:

```powershell
$dsl = @'
RemoteColumn(
    modifier = RemoteModifier
        .fillMaxWidth()
        .padding(16.rdp)
        .background(Color(0xFFE0F7FA).rc)
) {
    RemoteText(
        text = "Hello",
        color = Color.Black.rc,
        fontSize = 24.rsp,
        fontWeight = FontWeight.Bold
    )
}
'@

Invoke-RestMethod -Uri "http://localhost:8080/compile-rc" -Method Post -Body $dsl -ContentType "text/plain" -OutFile output.rc
```

Or with curl:

```bash
curl -X POST http://localhost:8080/compile-rc \
  -H "Content-Type: text/plain" \
  -d 'RemoteColumn(modifier = RemoteModifier.fillMaxWidth()) { RemoteText(text = "Hello", fontSize = 24.rsp) }' \
  -o output.rc
```

On success, the response is `application/octet-stream` containing the compiled RC binary. On error, the response body contains the compilation error message.

## Project Structure

```
rc_writer/
├── build.gradle.kts            # Build config: Ktor, Kotlin scripting, RemoteCompose deps
├── settings.gradle.kts         # Gradle project settings
├── gradle.properties           # Kotlin/Maven repo config
├── gradlew / gradlew.bat       # Gradle wrapper
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── Application.kt  # Ktor server — single endpoint: POST /compile-rc
│       └── resources/
│           └── logback.xml     # Logging configuration
└── .gitignore
```

### Key File: `src/main/kotlin/Application.kt`

The entire server lives in this single file. It:

1. Starts a Ktor/Netty server on port 8080.
2. Exposes `POST /compile-rc` which accepts a Kotlin DSL snippet as plain text.
3. Wraps the snippet with imports, wrapper functions, and a `createRcBuffer(profile) { ... }` call to form a complete Kotlin script.
4. Evaluates the script using the Kotlin JSR-223 scripting engine (`kotlin-scripting-jsr223`).
5. Returns the compiled `ByteArray` as `application/octet-stream`.

### Wrapper Functions

The server injects wrapper functions into the generated script because the user-facing Compose-layer API (`RemoteColumn`, `RemoteModifier`, `.rc`, etc.) uses `@Composable` functions that cannot run in a plain Kotlin script engine. The wrappers bridge to the procedural DSL (`Column`, `Text`, `Spacer`, `Modifier`) from `remote-creation-core`.

| Compose-layer API | Procedural DSL |
|---|---|
| `RemoteColumn { }` | `Column { }` |
| `RemoteRow { }` | `Row(horizontal = ...) { }` |
| `RemoteBox { }` | `Box(horizontal = Center, vertical = Center) { }` |
| `RemoteText(...)` | `Text(...)` with `FontWeight` to `Float` conversion |
| `RemoteSpacer(...)` | `Spacer(...)` |
| `RemoteModifier` | `Modifier` |
| `Color.rc` | `Color.value.toLong()` |
| `.clickable("name")` | `onClick { hostAction("name") }` |
| `.background(color, shape)` | `clip(shape).background(color)` |
| `.border(width, color, shape)` | `border(width, 0f, colorArgb, shapeInt)` |
| `.padding(horizontal, vertical)` | `padding(h, v, h, v)` |
| `RemoteRoundedCornerShape(r)` | `RoundedRectShape(r, r, r, r)` via clip |
| `RemoteArrangement.SpaceEvenly` | `RcRowHorizontalPositioning.SpaceEvenly` |
| `RemoteAlignment.Center` | `RcHorizontalPositioning.Center` + `RcVerticalPositioning.Center` |

## Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| Ktor server (Netty) | 3.0.3 | HTTP server |
| Kotlin scripting JSR-223 | 2.1.0 | Runtime Kotlin script compilation |
| `remote-core` | 1.0.0-alpha11 | RemoteCompose wire format |
| `remote-creation-core` | 1.0.0-alpha11 | Procedural DSL (`RcScope`, `Modifier`, `createRcBuffer`) |
| `remote-creation-jvm` | 1.0.0-alpha11 | JVM platform services |
| JetBrains Compose UI | 1.6.0 | `Color`, `FontWeight` types |

## Supported DSL Elements

- **Layouts:** `RemoteColumn`, `RemoteRow`, `RemoteBox`
- **Content:** `RemoteText`, `RemoteSpacer`
- **Modifiers:** `RemoteModifier.fillMaxWidth()`, `.fillMaxSize()`, `.padding()`, `.background()`, `.border()`, `.height()`, `.width()`, `.clickable()`, `.clip()`
- **Shapes:** `RemoteRoundedCornerShape(radius)`
- **Types:** `Color(x).rc`, `FontWeight.Bold/Light/Normal/Medium`, `.rdp` (dp), `.rsp` (sp)
- **Arrangement:** `RemoteArrangement.SpaceEvenly/SpaceBetween/SpaceAround/Start/End`
- **Alignment:** `RemoteAlignment.Center`
