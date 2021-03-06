import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.Jar
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    `java-library`
    kotlin("jvm") // version "1.3.72"
}

val camelVersion = "2.23.2"
val irohaJavaVersion = "6.2.0"

dependencies {
    api(project(":iroha-balancer-core"))
    api(project(":iroha-balancer-client"))
    testApi(project(":iroha-balancer-core").dependencyProject.sourceSets.test.get().output)

    // testcontainers
    testCompile("org.testcontainers:testcontainers:1.14.3")
    testImplementation("org.testcontainers:junit-jupiter:1.14.3")
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testCompile("org.junit.jupiter:junit-jupiter-engine:5.6.2")

    //Iroha libs
    testImplementation("com.github.hyperledger.iroha-java:client:6.2.0")
    testCompile("com.github.hyperledger.iroha-java:testcontainers:6.2.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

tasks.test {
    useJUnitPlatform()
    dependsOn(":iroha-balancer-core:dockerfileCreate")
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
