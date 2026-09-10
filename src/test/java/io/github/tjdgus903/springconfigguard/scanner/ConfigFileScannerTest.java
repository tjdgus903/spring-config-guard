package io.github.tjdgus903.springconfigguard.scanner;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFileScannerTest {
    private final ConfigFileScanner scanner = new ConfigFileScanner();

    @Test
    void nestedYamlIsFlattenedAndSourceLineIsPreserved() {
        String yaml = """
                spring:
                  jpa:
                    hibernate:
                      ddl-auto: create
                management:
                  endpoints:
                    web:
                      exposure:
                        include:
                          - health
                          - "*"
                """;

        List<ConfigEntry> entries = scanner.scan(yaml, "application-prod.yml", "prod");

        ConfigEntry ddl = find(entries, "spring.jpa.hibernate.ddl-auto");
        assertEquals("create", ddl.value());
        assertEquals("prod", ddl.profile());
        assertEquals("application-prod.yml", ddl.filePath());
        assertEquals(4, ddl.line());

        ConfigEntry exposure = find(entries, "management.endpoints.web.exposure.include");
        assertEquals("health,*", exposure.value());
        assertEquals(10, exposure.line());
    }

    @Test
    void yamlExtensionIsCaseInsensitive() {
        String yaml = "server:\n  port: 8080\n";
        List<ConfigEntry> entries = scanner.scan(yaml, "APPLICATION.YAML", null);
        assertEquals("8080", find(entries, "server.port").value());
    }

    @Test
    void propertiesSupportsEscapesAndContinuation() {
        String properties = """
                spring.jpa.hibernate.ddl-auto=create
                app.message=hello\\
                  world
                escaped\\ key=value
                """;

        List<ConfigEntry> entries = scanner.scan(properties, "application.properties", "default");

        assertEquals("create", find(entries, "spring.jpa.hibernate.ddl-auto").value());

        ConfigEntry message = find(entries, "app.message");
        assertEquals("helloworld", message.value());
        assertEquals(2, message.line());

        assertEquals("value", find(entries, "escaped key").value());
    }

    @Test
    void commentsAndBlankPropertiesLinesAreIgnored() {
        String properties = "# comment\n\n! another comment\nserver.port=8080\n";
        List<ConfigEntry> entries = scanner.scan(properties, "application.properties", null);
        assertEquals(1, entries.size());
        assertEquals("server.port", entries.getFirst().key());
        assertEquals(4, entries.getFirst().line());
    }

    @Test
    void unsupportedFileTypeIsRejected() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> scanner.scan("{}", "application.json", "prod")
        );
        assertTrue(exception.getMessage().contains("Unsupported"));
    }

    private ConfigEntry find(List<ConfigEntry> entries, String key) {
        return entries.stream()
                .filter(entry -> key.equals(entry.key()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing key: " + key));
    }
}
