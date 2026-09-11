package io.github.tjdgus903.springconfigguard.drift;

import io.github.tjdgus903.springconfigguard.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileDriftReportFormatterTest {

    private final ProfileDriftReportFormatter formatter = new ProfileDriftReportFormatter();

    @Test
    void formatsRiskAndDifferenceCountsWithRiskRuleId() {
        EffectiveConfigValue inherited = new EffectiveConfigValue(
                "payment.api.url",
                "prod",
                "http://localhost:8080",
                true,
                "default",
                "application.yml",
                2
        );
        EffectiveConfigValue overridden = new EffectiveConfigValue(
                "payment.timeout",
                "prod",
                "1000",
                false,
                "prod",
                "application-prod.yml",
                3
        );

        ProfileDriftAnalysis analysis = new ProfileDriftAnalysis(
                List.of(inherited, overridden),
                List.of(
                        new ProfileDriftFinding(
                                "SCG-PD001",
                                ProfileDriftKind.RISK,
                                Severity.HIGH,
                                inherited.key(),
                                inherited.profile(),
                                "Production profile inherits a local development endpoint",
                                "details",
                                inherited
                        ),
                        new ProfileDriftFinding(
                                null,
                                ProfileDriftKind.DIFFERENCE,
                                Severity.INFO,
                                overridden.key(),
                                overridden.profile(),
                                "Profile value differs from default configuration",
                                "details",
                                overridden
                        )
                )
        );

        String report = formatter.format(analysis);

        assertTrue(report.contains("Risk findings: 1"));
        assertTrue(report.contains("Profile differences: 1"));
        assertTrue(report.contains("[HIGH] [SCG-PD001] prod :: payment.api.url"));
        assertTrue(report.contains("[INFO] prod :: payment.timeout"));
    }

    @Test
    void explainsWhenNoApplicationConfigurationWasFound() {
        String report = formatter.format(new ProfileDriftAnalysis(List.of(), List.of()));
        assertTrue(report.contains("No Spring Boot application configuration files"));
    }
}
