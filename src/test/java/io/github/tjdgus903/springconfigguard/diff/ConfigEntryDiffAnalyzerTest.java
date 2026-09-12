package io.github.tjdgus903.springconfigguard.diff;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigEntryDiffAnalyzerTest {
    private final ConfigEntryDiffAnalyzer analyzer = new ConfigEntryDiffAnalyzer();

    @Test
    void classifiesAddedModifiedAndRemovedEntriesInStableIdentityOrder() {
        ConfigDiffAnalysis analysis = analyzer.analyze(
                List.of(
                        entry("obsolete.key", "old", "default", "application.yml", 2),
                        entry("server.error.include-stacktrace", "never", "prod", "application-prod.properties", 7)),
                List.of(
                        entry("logging.level.root", "DEBUG", "prod", "application-prod.properties", 3),
                        entry("server.error.include-stacktrace", "always", "prod", "application-prod.properties", 1))
        );

        assertEquals(List.of(ConfigChangeKind.ADDED, ConfigChangeKind.MODIFIED, ConfigChangeKind.REMOVED),
                analysis.changes().stream().map(ConfigChange::kind).toList());
        assertEquals("logging.level.root", analysis.additions().get(0).currentEntry().key());
        assertEquals("server.error.include-stacktrace", analysis.modifications().get(0).currentEntry().key());
        assertEquals("obsolete.key", analysis.removals().get(0).before().key());
        assertNull(analysis.removals().get(0).currentEntry());
    }

    @Test
    void ignoresLineOnlyMovementAndKeepsDuplicateOccurrences() {
        ConfigDiffAnalysis analysis = analyzer.analyze(
                List.of(
                        entry("feature.enabled", "true", "default", "application.yml", 2),
                        entry("feature.enabled", "false", "default", "application.yml", 3)),
                List.of(
                        entry("feature.enabled", "true", "default", "application.yml", 20),
                        entry("feature.enabled", "true", "default", "application.yml", 21),
                        entry("feature.enabled", "false", "default", "application.yml", 22))
        );

        assertEquals(List.of(ConfigChangeKind.MODIFIED, ConfigChangeKind.ADDED),
                analysis.changes().stream().map(ConfigChange::kind).toList());
        assertEquals(21, analysis.modifications().get(0).currentEntry().line());
        assertEquals(22, analysis.additions().get(0).currentEntry().line());
    }

    @Test
    void treatsTheSameKeyInDifferentProfilesAsIndependentEntries() {
        ConfigDiffAnalysis analysis = analyzer.analyze(
                List.of(entry("service.url", "http://localhost", "default", "application.yml", 1)),
                List.of(entry("service.url", "https://service.example", "prod", "application-prod.yml", 1))
        );

        assertEquals(2, analysis.changes().size());
        assertEquals(1, analysis.additions().size());
        assertEquals(1, analysis.removals().size());
        assertTrue(analysis.modifications().isEmpty());
    }

    private static ConfigEntry entry(String key, String value, String profile, String filePath, int line) {
        return new ConfigEntry(key, value, profile, filePath, line);
    }
}
