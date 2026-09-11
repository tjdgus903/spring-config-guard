package io.github.tjdgus903.springconfigguard.drift;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileDriftAnalyzerTest {

    private final ProfileDriftAnalyzer analyzer = new ProfileDriftAnalyzer();

    @Test
    void productionInheritsLocalhostDefaultAsRisk() {
        ProfileDriftAnalysis analysis = analyzer.analyze(List.of(
                entry("payment.api.url", "http://localhost:8080", "default", "application.yml", 2),
                entry("payment.api.url", "https://dev.payment.example", "dev", "application-dev.yml", 2),
                entry("logging.level.root", "INFO", "prod", "application-prod.yml", 1)
        ));

        EffectiveConfigValue prodValue = analysis.effectiveValue("prod", "payment.api.url").orElseThrow();
        assertEquals("http://localhost:8080", prodValue.value());
        assertTrue(prodValue.inherited());
        assertEquals("default", prodValue.sourceProfile());

        ProfileDriftFinding risk = analysis.findings().stream()
                .filter(finding -> "SCG-PD001".equals(finding.ruleId()))
                .findFirst()
                .orElseThrow();

        assertEquals(ProfileDriftKind.RISK, risk.kind());
        assertEquals(Severity.HIGH, risk.severity());
        assertEquals("prod", risk.profile());
        assertEquals("payment.api.url", risk.key());
    }

    @Test
    void explicitProductionEndpointOverrideAvoidsLocalInheritanceRisk() {
        ProfileDriftAnalysis analysis = analyzer.analyze(List.of(
                entry("payment.api.url", "http://localhost:8080", "default", "application.yml", 2),
                entry("payment.api.url", "https://payments.example", "prod", "application-prod.yml", 2)
        ));

        EffectiveConfigValue prodValue = analysis.effectiveValue("prod", "payment.api.url").orElseThrow();
        assertFalse(prodValue.inherited());
        assertEquals("https://payments.example", prodValue.value());
        assertFalse(analysis.findings().stream().anyMatch(finding -> "SCG-PD001".equals(finding.ruleId())));
        assertTrue(analysis.findings().stream().anyMatch(finding -> finding.kind() == ProfileDriftKind.DIFFERENCE));
    }

    @Test
    void nonProductionProfileCanInheritLocalEndpointWithoutRiskFinding() {
        ProfileDriftAnalysis analysis = analyzer.analyze(List.of(
                entry("payment.api.url", "http://localhost:8080", "default", "application.yml", 1),
                entry("feature.flag", "true", "dev", "application-dev.yml", 1)
        ));

        assertTrue(analysis.effectiveValue("dev", "payment.api.url").orElseThrow().inherited());
        assertFalse(analysis.findings().stream().anyMatch(finding -> "SCG-PD001".equals(finding.ruleId())));
    }

    @Test
    void plainProfileDifferenceIsInformationalAndNotRuleBacked() {
        ProfileDriftAnalysis analysis = analyzer.analyze(List.of(
                entry("payment.timeout", "5000", "default", "application.properties", 1),
                entry("payment.timeout", "1000", "prod", "application-prod.properties", 1)
        ));

        ProfileDriftFinding difference = analysis.findings().stream()
                .filter(finding -> finding.kind() == ProfileDriftKind.DIFFERENCE)
                .findFirst()
                .orElseThrow();

        assertEquals(Severity.INFO, difference.severity());
        assertEquals(null, difference.ruleId());
        assertEquals("1000", difference.effectiveValue().value());
        assertFalse(difference.effectiveValue().inherited());
    }

    @Test
    void unrelatedInheritedLocalStringDoesNotTriggerEndpointRule() {
        ProfileDriftAnalysis analysis = analyzer.analyze(List.of(
                entry("banner.message", "localhost developer machine", "default", "application.yml", 1),
                entry("feature.flag", "true", "production", "application-production.yml", 1)
        ));

        assertFalse(analysis.findings().stream().anyMatch(finding -> "SCG-PD001".equals(finding.ruleId())));
    }

    @Test
    void laterDuplicateEntryWinsForSameProfileAndKey() {
        ProfileDriftAnalysis analysis = analyzer.analyze(List.of(
                entry("payment.api.url", "http://localhost:8080", "default", "application.yml", 1),
                entry("payment.api.url", "https://old.example", "prod", "application-prod.yml", 1),
                entry("payment.api.url", "https://new.example", "prod", "application-prod.properties", 1)
        ));

        assertEquals(
                "https://new.example",
                analysis.effectiveValue("prod", "payment.api.url").orElseThrow().value()
        );
    }

    private static ConfigEntry entry(String key, String value, String profile, String file, int line) {
        return new ConfigEntry(key, value, profile, file, line);
    }
}
