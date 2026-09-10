package io.github.tjdgus903.springconfigguard.scanner;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigProfileDetectorTest {
    private final ConfigProfileDetector detector = new ConfigProfileDetector();

    @Test
    void prodAliasesAreDetectedAsProduction() {
        assertProduction("application-prod.yml", "prod");
        assertProduction("application-production.properties", "production");
        assertProduction("src/main/resources/application-prd.yaml", "prd");
    }

    @Test
    void matchingIsCaseInsensitive() {
        ConfigProfile profile = detector.detect("APPLICATION-PROD.YML").orElseThrow();
        assertTrue(profile.production());
        assertEquals("PROD", profile.name());
    }

    @Test
    void defaultAndDevelopmentProfilesAreNotProduction() {
        ConfigProfile defaultProfile = detector.detect("application.yml").orElseThrow();
        assertEquals("default", defaultProfile.name());
        assertFalse(defaultProfile.production());

        ConfigProfile dev = detector.detect("application-dev.yml").orElseThrow();
        assertEquals("dev", dev.name());
        assertFalse(dev.production());
    }

    @Test
    void supportsCustomProductionAliases() {
        ConfigProfileDetector custom = new ConfigProfileDetector(Set.of("live", "real"));
        assertTrue(custom.detect("application-live.yml").orElseThrow().production());
        assertFalse(custom.detect("application-prod.yml").orElseThrow().production());
    }

    @Test
    void rejectsNonSpringConfigurationFilenames() {
        assertTrue(detector.detect("bootstrap-prod.yml").isEmpty());
        assertTrue(detector.detect("application.json").isEmpty());
        assertTrue(detector.detect("").isEmpty());
    }

    private void assertProduction(String filePath, String expectedProfile) {
        ConfigProfile profile = detector.detect(filePath).orElseThrow();
        assertEquals(expectedProfile, profile.name());
        assertTrue(profile.production());
    }
}
