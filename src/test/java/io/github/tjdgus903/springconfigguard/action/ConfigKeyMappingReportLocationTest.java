package io.github.tjdgus903.springconfigguard.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigKeyMappingReportLocationTest {
    @Test
    void parsesConfigLocation() {
        var location = ConfigKeyMappingReportLocation.parse(
                "  config [prod] src/main/resources/application-prod.yml:17").orElseThrow();
        assertEquals("src/main/resources/application-prod.yml", location.path());
        assertEquals(17, location.line());
    }

    @Test
    void parsesValueLocationWithoutDefaultSuffix() {
        var location = ConfigKeyMappingReportLocation.parse(
                "  @Value src/main/java/example/DemoProperties.java:42 (default present)").orElseThrow();
        assertEquals("src/main/java/example/DemoProperties.java", location.path());
        assertEquals(42, location.line());
    }

    @Test
    void parsesConfigurationPropertiesLocationWithoutDeclaringMetadata() {
        var location = ConfigKeyMappingReportLocation.parse(
                "  @ConfigurationProperties src/main/java/example/AppProperties.java:9 :: example.AppProperties#name")
                .orElseThrow();
        assertEquals("src/main/java/example/AppProperties.java", location.path());
        assertEquals(9, location.line());
    }

    @Test
    void parsesWindowsAbsolutePath() {
        var location = ConfigKeyMappingReportLocation.parse(
                "  @Value C:\\work\\demo\\src\\main\\java\\example\\Demo.java:73").orElseThrow();
        assertEquals("C:\\work\\demo\\src\\main\\java\\example\\Demo.java", location.path());
        assertEquals(73, location.line());
    }

    @Test
    void ignoresHeadingsAndInvalidLines() {
        assertTrue(ConfigKeyMappingReportLocation.parse("Matched keys:").isEmpty());
        assertTrue(ConfigKeyMappingReportLocation.parse("- demo.feature.enabled").isEmpty());
        assertTrue(ConfigKeyMappingReportLocation.parse("  ... 4 more omitted").isEmpty());
        assertTrue(ConfigKeyMappingReportLocation.parse("  @Value src/main/java/example/Demo.java:0").isEmpty());
        assertTrue(ConfigKeyMappingReportLocation.parse("  @Value src/main/java/example/Demo.java:not-a-line").isEmpty());
    }
}
