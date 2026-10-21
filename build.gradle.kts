plugins {
    kotlin("jvm") version "1.9.24"
    application
}

group = "com.exemplo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val lwjglVersion = "3.3.3"
val imguiVersion = "1.86.10"

val osName = System.getProperty("os.name").lowercase()
val lwjglNatives = when {
    osName.contains("win") -> "natives-windows"
    osName.contains("mac") -> "natives-macos"
    else -> "natives-linux"
}

dependencies {
    implementation(enforcedPlatform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")

    // Biblioteca de Matemática 3D
    implementation("org.joml:joml:1.10.5")

    // Dear ImGui para Kotlin/Java
    implementation("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
    implementation("io.github.spair:imgui-java-app:$imguiVersion")
}

application {
    mainClass.set("MainKt")
}

// Tarefa para juntar o código e todas as dependências num único .jar
tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}