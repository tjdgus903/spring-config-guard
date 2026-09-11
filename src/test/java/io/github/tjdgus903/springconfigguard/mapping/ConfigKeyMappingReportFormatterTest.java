package io.github.tjdgus903.springconfigguard.mapping;

import io.github.tjdgus903.springconfigguard.code.ConfigUsage;
import io.github.tjdgus903.springconfigguard.code.ConfigurationPropertyMapping;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigKeyMappingReportFormatterTest {
    private final ConfigKeyMappingReportFormatter formatter = new ConfigKeyMappingReportFormatter();
    private final ConfigFileJavaKeyMatcher matcher = new ConfigFileJavaKeyMatcher();

    @Test
    void reportsLocationsProfilesAndCountsWithoutValuesExpressionsOrDefaults() {
        ConfigKeyMappingAnalysis analysis = matcher.match(
                List.of(
                        new ConfigEntry("service.url", "CONFIG_SECRET", "default", "application.yml", 2),
                        new ConfigEntry("service.url", "PROD_SECRET", "prod", "application-prod.properties", 7),
                        new ConfigEntry("config.only", "UNMATCHED_CONFIG_SECRET", "default", "application.yml", 8)),
                List.of(
                        new ConfigUsage("service.url", "DEFAULT_SECRET", "EXPRESSION_SECRET", "Client.java", 4),
                        new ConfigUsage("external.required", null, "UNMATCHED_EXPRESSION_SECRET", "External.java", 9),
                        new ConfigUsage("external.optional", "UNMATCHED_DEFAULT_SECRET", "", "External.java", 10)),
                List.of(
                        mapping("service.url", "ServiceProperties.java", 5),
                        mapping("payment.region", "PaymentProperties.java", 6))
        );

        String report = formatter.format(analysis);

        assertTrue(report.contains("Matched keys: 1"));
        assertTrue(report.contains("Config entries without an exact Java reference: 1"));
        assertTrue(report.contains("@Value references without an exact config entry: 2"));
        assertTrue(report.contains("@ConfigurationProperties fields without an exact config entry: 1"));
        assertTrue(report.contains("config [default] application.yml:2"));
        assertTrue(report.contains("config [prod] application-prod.properties:7"));
        assertTrue(report.contains("@Value Client.java:4 (default present)"));
        assertTrue(report.contains("@Value External.java:9\n"));
        assertTrue(report.contains("@ConfigurationProperties ServiceProperties.java:5 :: com.acme.Properties#field"));
        assertTrue(report.contains("Unmatched occurrences are informational"));
        assertFalse(report.contains("SECRET"));
    }

    @Test
    void preservesJavaOnlyInventoryAndMarksAnEmptyDefaultAsPresent() {
        String report = formatter.format(matcher.match(List.of(),
                List.of(new ConfigUsage("external.key", "", "${external.key:}", "Client.java", 3)), List.of()));

        assertTrue(report.contains("Matched keys: 0"));
        assertTrue(report.contains("@Value references without an exact config entry: 1"));
        assertTrue(report.contains("- external.key"));
        assertTrue(report.contains("Client.java:3 (default present)"));
        assertFalse(report.contains("${external.key:}"));
        assertFalse(report.contains("No supported configuration entries or Java references"));
    }

    @Test
    void explainsAnEmptyInventoryWithoutClaimingThatConfigurationFilesDoNotExist() {
        String report = formatter.format(new ConfigKeyMappingAnalysis(List.of(), List.of(), List.of(), List.of()));

        assertTrue(report.contains("No supported configuration entries or Java references were found."));
        assertTrue(report.contains("Framework binding, environment values and external configuration are not resolved."));
    }

    @Test
    void boundsAllSectionsAndPerKeyLocationsWhilePreservingTotalsAndKeyOrder() {
        List<ConfigEntry> entries = new ArrayList<>();
        List<ConfigUsage> usages = new ArrayList<>();
        List<ConfigurationPropertyMapping> mappings = new ArrayList<>();
        for (int i = 22; i >= 0; i--) {
            String suffix = "%02d".formatted(i);
            entries.add(new ConfigEntry("matched." + suffix, "", "default", "application.properties", i + 1));
            entries.add(new ConfigEntry("config-only." + suffix, "", "default", "application.properties", i + 30));
            usages.add(new ConfigUsage("matched." + suffix, null, "", "Client.java", i + 1));
            usages.add(new ConfigUsage("value-only." + suffix, null, "", "Client.java", i + 30));
            mappings.add(mapping("field-only." + suffix, "Properties.java", i + 1));
        }
        IntStream.range(0, 7).forEach(i -> entries.add(
                new ConfigEntry("matched.00", "", "prod", "application-prod.properties", i + 1)));

        String report = formatter.format(matcher.match(entries, usages, mappings));

        assertTrue(report.contains("Matched keys: 23"));
        assertTrue(report.contains("Config entries without an exact Java reference: 23"));
        assertTrue(report.contains("@Value references without an exact config entry: 23"));
        assertTrue(report.contains("@ConfigurationProperties fields without an exact config entry: 23"));
        assertTrue(report.indexOf("- matched.00") < report.indexOf("- matched.01"));
        for (String prefix : List.of("matched.", "config-only.", "value-only.", "field-only.")) {
            assertTrue(report.contains("- " + prefix + "19\n"));
            assertFalse(report.contains("- " + prefix + "20\n"));
        }
        assertTrue(report.contains("... 3 more omitted"));
        assertTrue(report.contains("application-prod.properties:4\n"));
        assertFalse(report.contains("application-prod.properties:5\n"));
    }

    @Test
    void keepsUntrustedLabelsOnOneLineAndBoundsLongPaths() {
        String path = "folder\nnext\r\t" + "x".repeat(500);
        String report = formatter.format(matcher.match(
                List.of(new ConfigEntry("config.key", "", "default", path, 7)), List.of(), List.of()));

        assertTrue(report.contains("folder next  "));
        assertTrue(report.contains("...:7"));
        assertFalse(report.contains("folder\nnext"));
        assertFalse(report.contains("x".repeat(241)));
    }

    private static ConfigurationPropertyMapping mapping(String key, String file, int line) {
        return new ConfigurationPropertyMapping(key, "", "com.acme.Properties", "field", file, line);
    }
}
