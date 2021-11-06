import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.5.6"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.spring") version "1.5.31"
}

group = "com.vranisimo"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("com.google.cloud:google-cloud-storage:2.1.9")

    implementation("io.ktor:ktor-auth:1.6.4")
    implementation("io.ktor:ktor-auth-jwt:1.6.4")

    implementation("org.springframework.kafka:spring-kafka:2.7.8")

    implementation ("org.springframework.boot:spring-boot-starter-security")
    implementation("javax.persistence:javax.persistence-api:2.2")

    implementation("io.jsonwebtoken:jjwt:0.9.1")
    implementation("javax.xml.bind:jaxb-api:2.3.1")



//    compile(kotlin("stdlib"))
//    compile(kotlin("reflect"))
//    implementation("org.apache.kafka:kafka-clients:2.1.0")
//    implementation("org.apache.kafka:kafka-streams:2.1.0")
//    implementation("org.apache.kafka:connect-runtime:2.1.0")
//    implementation("io.confluent:kafka-json-serializer:5.0.1")
//    implementation("org.slf4j:slf4j-api:1.7.6")
//    implementation("org.slf4j:slf4j-log4j12:1.7.6")
//    implementation("com.fasterxml.jackson.core:jackson-databind:[2.8.11.1,)")
//    implementation("com.google.code.gson:gson:2.2.4")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
