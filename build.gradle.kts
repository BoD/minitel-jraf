
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.bmuschko.gradle.docker.tasks.image.Dockerfile.CopyFileInstruction

plugins {
  alias(libs.plugins.kotlin.jvm)
  id("application")
  alias(libs.plugins.dockerJavaApplication)
  alias(libs.plugins.apollo)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.tapmoc)
}

group = "org.jraf"
version = "1.0.0"

kotlin {
  compilerOptions {
    optIn.add("kotlin.time.ExperimentalTime")
  }
}

application {
  mainClass.set("org.jraf.miniteljraf.main.MainKt")
}

dependencies {
  // Ktor
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.ktor.server.defaultHeaders)
  implementation(libs.ktor.server.statusPages)
  implementation(libs.ktor.server.contentNegotiation)
  implementation(libs.ktor.server.websockets)
  implementation(libs.ktor.serialization.kotlinx.json)

  // Minitel
  implementation(libs.klibminitel)

  // Apollo
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.adapters.core)

  // Logging
  implementation(libs.klibnanolog)
  implementation(libs.logback)

  // JSON
  implementation(libs.kotlinx.serialization.json)

  // Markdown
  implementation(libs.markdown)

  // Slack
  implementation(libs.klibslack)

  // JSoup
  implementation(libs.jsoup)
}

docker {
  javaApplication {
    baseImage.set("eclipse-temurin:25")
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
  // Install curl
  runCommand("apt-get update")
  runCommand("apt-get install -y curl")

  // Download the OpenTelemetry Java agent
  runCommand("curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar")

  // OpenTelemetry Java agent configuration
  environmentVariable(
    mapOf(
      "JAVA_TOOL_OPTIONS" to "-javaagent:opentelemetry-javaagent.jar",
      "OTEL_SERVICE_NAME" to rootProject.name.lowercase(),
    )
  )

  // Move the COPY instructions to the end
  // See https://github.com/bmuschko/gradle-docker-plugin/issues/1093
  instructions.set(
    instructions.get().sortedBy { instruction ->
      if (instruction.keyword == CopyFileInstruction.KEYWORD) 1 else 0
    }
  )
}

apollo {
  service("github") {
    packageName.set("org.jraf.miniteljraf.github")
    srcDir("src/main/graphql/github")
    mapScalar("DateTime", "kotlin.time.Instant", "com.apollographql.adapter.core.KotlinInstantAdapter")
    introspection {
      endpointUrl.set("https://api.github.com/graphql")
      schemaFile.set(file("src/main/graphql/github/schema.graphqls"))
      // Add `githubOauthKey` to your ~/.gradle/gradle.properties file
      headers.put("Authorization", "Bearer ${project.findProperty("githubOauthKey")}")
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

tapmoc {
  java(25)
  kotlin(libs.versions.kotlin.get())
  checkDependencies()
}

// `./gradlew downloadGithubApolloSchemaFromIntrospection downloadResumeApolloSchemaFromIntrospection` to download the schemas
// `./gradlew distZip` to create a zip distribution
// `./gradlew refreshVersions` to update dependencies
// `DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage` to build and push the image
