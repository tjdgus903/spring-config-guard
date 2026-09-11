plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    // Real Spring annotation types for IDE resolution. This sample starts no application.
    compileOnly("org.springframework.boot:spring-boot:4.1.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
