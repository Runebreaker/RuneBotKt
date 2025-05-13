import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    application
}

group = "de.runebot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    // Kord Snapshots Repository (Optional):
    maven("https://oss.sonatype.org/content/repositories/snapshots")

    // Crunch eval lib
    maven("https://redempt.dev")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("dev.kord:kord-core:0.15.0")
    implementation("dev.kord.x:emoji:0.5.0")

    implementation("org.jetbrains.exposed:exposed-core:0.46.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.46.0")
    implementation("org.xerial:sqlite-jdbc:3.45.0.0")

    implementation("info.debatty:java-string-similarity:2.0.0")

    implementation("com.twelvemonkeys.imageio:imageio-core:3.10.1")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.10.1")

    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.jclarion:image4j:0.7")

    implementation("org.jetbrains.exposed:exposed-java-time:0.46.0")

    implementation("com.github.Redempt:Crunch:1.1.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}

application {
    mainClass.set("de.runebot.RuneBot")
}