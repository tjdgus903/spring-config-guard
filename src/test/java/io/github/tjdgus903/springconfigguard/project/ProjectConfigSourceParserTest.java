package io.github.tjdgus903.springconfigguard.project;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectConfigSourceParserTest {

    private final ProjectConfigSourceParser parser = new ProjectConfigSourceParser();

    @Test
    void parsesYamlAndPropertiesAndIgnoresUnrelatedFiles() {
        List<ConfigEntry> entries = parser.parse(List.of(
                new ProjectConfigSource(
                        "src/main/resources/application.yml",
                        "payment:\n  api:\n    url: http://localhost:8080\n"
                ),
                new ProjectConfigSource(
                        "src/main/resources/application-prod.properties",
                        "payment.timeout=1000\n"
                ),
                new ProjectConfigSource("README.md", "payment.timeout=not-config\n")
        ));

        assertTrue(entries.stream().anyMatch(entry ->
                entry.profile().equals("default")
                        && entry.key().equals("payment.api.url")
                        && entry.value().equals("http://localhost:8080")
        ));
        assertTrue(entries.stream().anyMatch(entry ->
                entry.profile().equals("prod")
                        && entry.key().equals("payment.timeout")
                        && entry.value().equals("1000")
        ));
        assertFalse(entries.stream().anyMatch(entry -> "not-config".equals(entry.value())));
    }

    @Test
    void malformedConfigDoesNotPreventOtherSourcesFromBeingParsed() {
        List<ConfigEntry> entries = parser.parse(List.of(
                new ProjectConfigSource(
                        "application-prod.yml",
                        "spring: [broken\n"
                ),
                new ProjectConfigSource(
                        "application-prod.properties",
                        "logging.level.root=DEBUG\n"
                )
        ));

        assertTrue(entries.stream().anyMatch(entry ->
                entry.profile().equals("prod")
                        && entry.key().equals("logging.level.root")
                        && entry.value().equals("DEBUG")
        ));
    }

    @Test
    void nullSourcesAreIgnored() {
        List<ConfigEntry> entries = parser.parse(java.util.Arrays.asList(
                null,
                new ProjectConfigSource(null, "ignored"),
                new ProjectConfigSource("application-prod.properties", null),
                new ProjectConfigSource("application-prod.properties", "spring.jpa.show-sql=true\n")
        ));

        assertTrue(entries.stream().anyMatch(entry ->
                entry.key().equals("spring.jpa.show-sql") && entry.value().equals("true")
        ));
    }
}
