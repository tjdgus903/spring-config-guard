package io.github.tjdgus903.springconfigguard.diff;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangedConfigRiskAnalyzerTest {
    private final ChangedConfigRiskAnalyzer analyzer = new ChangedConfigRiskAnalyzer();

    @Test
    void reportsRiskForAnAddedProductionEntry() {
        ConfigChange change = added("logging.level.root", "DEBUG", "prod");

        ChangedConfigRiskAnalysis analysis = analyzer.analyze(new ConfigDiffAnalysis(List.of(change)));

        assertEquals(1, analysis.findings().size());
        assertEquals(change, analysis.findings().getFirst().change());
        assertEquals("SCG004", analysis.findings().getFirst().finding().ruleId());
    }

    @Test
    void reportsRiskForTheCurrentValueOfAModifiedProductionEntry() {
        ConfigChange change = new ConfigChange(
                ConfigChangeKind.MODIFIED,
                entry("spring.jpa.show-sql", "false", "production"),
                entry("spring.jpa.show-sql", "true", "production")
        );

        ChangedConfigRiskAnalysis analysis = analyzer.analyze(new ConfigDiffAnalysis(List.of(change)));

        assertEquals(List.of("SCG005"), analysis.findings().stream()
                .map(item -> item.finding().ruleId()).toList());
        assertEquals("true", analysis.findings().getFirst().finding().entry().value());
    }

    @Test
    void ignoresRemovedAndNonProductionEntries() {
        ConfigChange removed = new ConfigChange(
                ConfigChangeKind.REMOVED, entry("logging.level.root", "DEBUG", "prod"), null);
        ConfigChange development = added("logging.level.root", "DEBUG", "dev");

        ChangedConfigRiskAnalysis analysis = analyzer.analyze(new ConfigDiffAnalysis(List.of(removed, development)));

        assertTrue(analysis.findings().isEmpty());
    }

    private static ConfigChange added(String key, String value, String profile) {
        return new ConfigChange(ConfigChangeKind.ADDED, null, entry(key, value, profile));
    }

    private static ConfigEntry entry(String key, String value, String profile) {
        return new ConfigEntry(key, value, profile, "application-" + profile + ".yml", 4);
    }
}
