package io.github.tjdgus903.springconfigguard.scanner;

/** Profile metadata derived from a Spring Boot configuration filename. */
public record ConfigProfile(String name, boolean production) {
}
