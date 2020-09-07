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
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-devtools")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Logging
    api("io.github.microutils:kotlin-logging:1.7.9")

    // Apache Commons
    implementation("org.apache.commons:commons-collections4:4.4")

    // RabbitMQ
    implementation("com.rabbitmq:amqp-client:5.9.0")

    // testcontainers
    testCompile("org.testcontainers:testcontainers:1.14.3")
    testImplementation("org.testcontainers:junit-jupiter:1.14.3")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")

    // Iroha java library
    implementation("com.github.hyperledger.iroha-java:client:${irohaJavaVersion}")

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
