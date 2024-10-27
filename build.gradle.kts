import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import java.io.FileInputStream
import java.util.Base64
import java.util.Properties

plugins {
  kotlin("jvm")
  id("application")
  id("com.bmuschko.docker-java-application")
}

group = "org.jraf"
version = "1.0.0"

repositories {
  mavenLocal()
  mavenCentral()
}

kotlin {
  jvmToolchain(11)
}

application {
  mainClass.set("org.jraf.miniteljraf.main.MainKt")
}

dependencies {
  // Ktor
  implementation(Ktor.server.core)
  implementation(Ktor.server.netty)
  implementation(Ktor.server.defaultHeaders)
  implementation(Ktor.server.statusPages)
  implementation(Ktor.server.contentNegotiation)
  implementation(Ktor.server.websockets)
  implementation(Ktor.plugins.serialization.kotlinx.json)

  // Minitel
  implementation("org.jraf:klibminitel:_")

  // Logback
  runtimeOnly("ch.qos.logback:logback-classic:_")
}

docker {
  javaApplication {
    maintainer.set("BoD <BoD@JRAF.org>")
    ports.set(listOf(8080))
    images.add("bodlulu/${rootProject.name}:latest")
    jvmArgs.set(listOf("-Xms16m", "-Xmx128m"))
  }
  registryCredentials {
    username.set(System.getenv("DOCKER_USERNAME"))
    password.set(System.getenv("DOCKER_PASSWORD"))
  }
}

tasks.withType<DockerBuildImage> {
  platform.set("linux/amd64")
}

tasks.withType<com.bmuschko.gradle.docker.tasks.image.Dockerfile> {
  environmentVariable("MALLOC_ARENA_MAX", "4")
}

// `./gradlew distZip` to create a zip distribution
// `./gradlew refreshVersions` to update dependencies
// `DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage` to build and push the image
