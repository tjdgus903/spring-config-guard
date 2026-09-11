package io.github.tjdgus903.springconfigguard.mapping;

import io.github.tjdgus903.springconfigguard.code.ConfigUsage;
import io.github.tjdgus903.springconfigguard.code.ConfigurationPropertyMapping;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFileJavaKeyMatcherTest {
    private final ConfigFileJavaKeyMatcher matcher = new ConfigFileJavaKeyMatcher();

    @Test
    void matchesValueAndConfigurationPropertiesReferencesByExactKey() {
        ConfigEntry endpoint = entry("payment.api.url", "https://payment.example", "prod", "application-prod.yml");
        ConfigEntry retries = entry("payment.max-retries", "3", "default", "application.yml");
        ConfigUsage endpointUsage = usage("payment.api.url", "PaymentClient.java");
        ConfigurationPropertyMapping retriesMapping = mapping("payment.max-retries", "PaymentProperties.java");

        ConfigKeyMappingAnalysis analysis = matcher.match(
                List.of(endpoint, retries),
                List.of(endpointUsage),
                List.of(retriesMapping)
        );

        assertEquals(List.of("payment.api.url", "payment.max-retries"),
                analysis.matches().stream().map(ConfigKeyMatch::key).toList());
        assertEquals(List.of(endpoint), analysis.matches().get(0).configEntries());
        assertEquals(List.of(endpointUsage), analysis.matches().get(0).valueUsages());
        assertEquals(List.of(retriesMapping), analysis.matches().get(1).propertyMappings());
        assertTrue(analysis.unmatchedConfigEntries().isEmpty());
        assertTrue(analysis.unmatchedValueUsages().isEmpty());
        assertTrue(analysis.unmatchedPropertyMappings().isEmpty());
    }

    @Test
    void reportsKeysThatExistOnOnlyOneSideWithoutGuessingRelaxedNames() {
        ConfigEntry unused = entry("feature.enabled", "true", "prod", "application-prod.properties");
        ConfigUsage missing = usage("service.timeout", "ServiceClient.java");
        ConfigurationPropertyMapping relaxedButNotExact = mapping("feature-enabled", "FeatureProperties.java");

        ConfigKeyMappingAnalysis analysis = matcher.match(
                List.of(unused),
                List.of(missing),
                List.of(relaxedButNotExact)
        );

        assertTrue(analysis.matches().isEmpty());
        assertEquals(List.of(unused), analysis.unmatchedConfigEntries());
        assertEquals(List.of(missing), analysis.unmatchedValueUsages());
        assertEquals(List.of(relaxedButNotExact), analysis.unmatchedPropertyMappings());
    }

    @Test
    void preservesDuplicateEntriesAndReferencesInInputOrder() {
        ConfigEntry defaultEntry = entry("service.url", "http://localhost", "default", "application.yml");
        ConfigEntry prodEntry = entry("service.url", "https://service.example", "prod", "application-prod.yml");
        ConfigUsage firstUsage = usage("service.url", "FirstClient.java");
        ConfigUsage secondUsage = usage("service.url", "SecondClient.java");

        ConfigKeyMappingAnalysis analysis = matcher.match(
                List.of(defaultEntry, prodEntry),
                List.of(firstUsage, secondUsage),
                List.of()
        );

        assertEquals(1, analysis.matches().size());
        assertEquals(List.of(defaultEntry, prodEntry), analysis.matches().get(0).configEntries());
        assertEquals(List.of(firstUsage, secondUsage), analysis.matches().get(0).valueUsages());
    }

    private static ConfigEntry entry(String key, String value, String profile, String filePath) {
        return new ConfigEntry(key, value, profile, filePath, 1);
    }

    private static ConfigUsage usage(String key, String filePath) {
        return new ConfigUsage(key, null, "${" + key + "}", filePath, 1);
    }

    private static ConfigurationPropertyMapping mapping(String key, String filePath) {
        int separator = key.lastIndexOf('.');
        String prefix = separator >= 0 ? key.substring(0, separator) : "";
        String fieldName = separator >= 0 ? key.substring(separator + 1) : key;
        return new ConfigurationPropertyMapping(key, prefix, "com.acme.Properties", fieldName, filePath, 1);
    }
}
