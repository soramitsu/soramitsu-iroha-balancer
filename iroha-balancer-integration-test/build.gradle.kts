import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.Jar
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    `java-library`
    kotlin("jvm") // version "1.3.72"
    kotlin("kapt") // version "1.3.72"
}

val camelVersion = "2.23.2"
val irohaJavaVersion = "6.2.0"

dependencies {
    // testcontainers
    compile("org.testcontainers:testcontainers:1.14.3")
    implementation("org.testcontainers:junit-jupiter:1.14.3")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

val jar: Jar by tasks
val bootJar: BootJar by tasks

bootJar.enabled = false
jar.enabled = true
repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
