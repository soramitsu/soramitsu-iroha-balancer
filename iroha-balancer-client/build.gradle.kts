import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.Jar
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    `java-library`
    kotlin("jvm") version "1.3.72"
    kotlin("kapt") version "1.3.72"
}

val camelVersion = "2.23.2"

dependencies {
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.7.9")

    // Apache Commons
    implementation("org.apache.commons:commons-collections4:4.4")

    // Apache Camel
    implementation("org.apache.camel:camel-spring-boot-starter:$camelVersion")
    implementation("org.apache.camel:camel-rabbitmq-starter:$camelVersion")
    implementation("org.apache.camel:camel-jackson-starter:$camelVersion")
    implementation("org.apache.camel:camel-bean-validator-starter:$camelVersion")
    testCompile("org.apache.camel:camel-test-spring:$camelVersion")

    // testcontainers
    testCompile("org.testcontainers:testcontainers:1.14.3")
    testImplementation("org.testcontainers:junit-jupiter:1.14.3")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")

    implementation("javax.servlet:javax.servlet-api:3.1.0")

    // TODO: Add Camel related dependencies

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