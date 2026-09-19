package io.github.tjdgus903.springconfigguard.diff;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangedConfigRiskReportFormatterTest {
    private final ChangedConfigRiskReportFormatter formatter = new ChangedConfigRiskReportFormatter();

    @Test
    void reportsRuleScopeAndFindingMetadataWithoutConfigurationValuesOrDescriptions() {
        ConfigEntry entry = new ConfigEntry(
                "spring.jpa.hibernate.ddl-auto", "SUPER_SECRET_VALUE", "prod", "application-prod.yml", 12);
        ConfigChange change = new ConfigChange(ConfigChangeKind.ADDED, null, entry);
        Finding finding = new Finding("SCG001", Severity.CRITICAL, "message with SUPER_SECRET_VALUE",
                "description with SUPER_SECRET_VALUE", entry);

        String report = formatter.format(new ConfigDiffAnalysis(List.of(change)),
                new ChangedConfigRiskAnalysis(List.of(new ChangedConfigRiskFinding(change, finding))), 10, 12);

        assertTrue(report.contains("Rules enabled: 10 of 12"));
        assertTrue(report.contains("[CRITICAL] [SCG001] spring.jpa.hibernate.ddl-auto (profile: prod) at application-prod.yml:12"));
        assertFalse(report.contains("SUPER_SECRET_VALUE"));
        assertFalse(report.contains("message with"));
        assertFalse(report.contains("description with"));
    }

    @Test
    void distinguishesNoChangesFromChangesWithNoFindingsAndAlwaysShowsScope() {
        assertEquals("""
                Changed Configuration Analysis

                Rules enabled: 0 of 12

                No local Spring Boot application configuration changes were found.""",
                formatter.format(new ConfigDiffAnalysis(List.of()), new ChangedConfigRiskAnalysis(List.of()), 0, 12));

        ConfigEntry entry = new ConfigEntry("feature.enabled", "true", "dev", "application-dev.yml", 3);
        String report = formatter.format(
                new ConfigDiffAnalysis(List.of(new ConfigChange(ConfigChangeKind.ADDED, null, entry))),
                new ChangedConfigRiskAnalysis(List.of()), 7, 12);

        assertTrue(report.contains("Rules enabled: 7 of 12"));
        assertTrue(report.contains("Changed entries: 1"));
        assertTrue(report.contains("No deterministic risk findings detected."));
    }

    @Test
    void rejectsInvalidRuleCounts() {
        ConfigDiffAnalysis diff = new ConfigDiffAnalysis(List.of());
        ChangedConfigRiskAnalysis risk = new ChangedConfigRiskAnalysis(List.of());

        assertThrows(IllegalArgumentException.class, () -> formatter.format(diff, risk, -1, 12));
        assertThrows(IllegalArgumentException.class, () -> formatter.format(diff, risk, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> formatter.format(diff, risk, 13, 12));
    }
}
