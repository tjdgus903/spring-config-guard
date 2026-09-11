import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "io.github.tjdgus903.springconfigguard"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("org.yaml:snakeyaml:2.7")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")

    intellijPlatform {
        intellijIdea("2026.1.3")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
    inputs.dir("samples/config-mapping/src/main")
    systemProperty("scg.sample.dir", layout.projectDirectory.dir("samples/config-mapping").asFile.absolutePath)
    testLogging {
        events = setOf(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.register<Zip>("buildSmokeTestSample") {
    group = "distribution"
    description = "Packages the standalone IDE smoke-test sample with the project's Gradle Wrapper."
    archiveFileName.set("spring-config-guard-sample.zip")
    destinationDirectory.set(layout.buildDirectory.dir("smoke-test"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    into("config-mapping") {
        from("samples/config-mapping") {
            include("README.md", "build.gradle.kts", "settings.gradle.kts", "src/main/**")
        }
        from("gradlew") {
            filePermissions { unix("rwxr-xr-x") }
        }
        from("gradlew.bat")
        into("gradle/wrapper") {
            from("gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties")
        }
    }
}
