import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.21"
}

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compile("no.tornado:tornadofx:1.7.18")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}