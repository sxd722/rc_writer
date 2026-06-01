import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.0"
    id("io.ktor.plugin") version "3.0.3"
    application
}

application {
    mainClass.set("ApplicationKt")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("ch.qos.logback:logback-classic:1.5.15")

    implementation("org.jetbrains.kotlin:kotlin-scripting-jsr223:2.1.0")

    val rcVersion = "1.0.0-alpha11"
    implementation("androidx.compose.remote:remote-core:$rcVersion")
    implementation("androidx.compose.remote:remote-creation-core:$rcVersion")
    implementation("androidx.compose.remote:remote-creation-jvm:$rcVersion")

    implementation("org.jetbrains.compose.ui:ui:1.6.0")
    implementation("org.jetbrains.compose.ui:ui-text:1.6.0")
    implementation("org.jetbrains.compose.ui:ui-graphics:1.6.0")
}

tasks.named<JavaExec>("run") {
    doFirst {
        systemProperty(
            "script.engine.classpath",
            sourceSets.main.get().runtimeClasspath.asPath
        )
    }
}
