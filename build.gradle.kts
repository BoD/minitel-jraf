import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile

plugins {
  kotlin("jvm")
  id("application")
  id("com.bmuschko.docker-java-application")
  id("com.apollographql.apollo")
  kotlin("plugin.serialization")
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
//  mainClass.set("org.jraf.miniteljraf.main.minitel.app.MinitelAppKt")
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

  // Apollo
  implementation("com.apollographql.apollo:apollo-runtime:_")
  implementation("com.apollographql.adapters:apollo-adapters-core:_")
  implementation("com.apollographql.adapters:apollo-adapters-kotlinx-datetime:_")

  // Logback
  implementation("org.slf4j:slf4j-simple:_")

  // JSON
  implementation(KotlinX.serialization.json)

  // Markdown
  implementation("org.jetbrains:markdown:_")

  // Slack
  implementation("org.jraf:klibslack:_")

  // JSoup
  implementation("org.jsoup:jsoup:_")
}

docker {
  javaApplication {
    // Use OpenJ9 instead of the default one
    baseImage.set("adoptopenjdk/openjdk11-openj9:x86_64-ubuntu-jre-11.0.24_8_openj9-0.46.1")
    mainClassName.set("org.jraf.miniteljraf.main.MainKt")
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

tasks.withType<Dockerfile> {
  environmentVariable("MALLOC_ARENA_MAX", "4")
}

apollo {
  service("github") {
    packageName.set("org.jraf.miniteljraf.github")
    srcDir("src/main/graphql/github")
    mapScalar("DateTime", "kotlinx.datetime.Instant", "com.apollographql.adapter.datetime.KotlinxInstantAdapter")
    introspection {
      endpointUrl.set("https://api.github.com/graphql")
      schemaFile.set(file("src/main/graphql/github/schema.graphqls"))
      // Add `githubOauthKey` to your ~/.gradle/gradle.properties file
      val githubOauthKey = project.findProperty("githubOauthKey")?.toString() ?: ""
      headers.put("Authorization", "Bearer $githubOauthKey")
    }
  }
  service("resume") {
    packageName.set("org.jraf.miniteljraf.resume")
    srcDir("src/main/graphql/resume")
    introspection {
      endpointUrl.set("http://server.jraf.org:4000")
      schemaFile.set(file("src/main/graphql/resume/schema.graphqls"))
    }
    mapScalarToKotlinString("Email")
  }
}

// `./gradlew distZip` to create a zip distribution
// `./gradlew refreshVersions` to update dependencies
// `DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage` to build and push the image
