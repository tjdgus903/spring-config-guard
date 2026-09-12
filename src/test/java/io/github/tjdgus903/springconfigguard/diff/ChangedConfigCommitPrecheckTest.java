package io.github.tjdgus903.springconfigguard.diff;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static io.github.tjdgus903.springconfigguard.diff.ChangedConfigCommitPrecheckResult.Recommendation.PROCEED;
import static io.github.tjdgus903.springconfigguard.diff.ChangedConfigCommitPrecheckResult.Recommendation.REVIEW_BEFORE_PROCEED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChangedConfigCommitPrecheckTest {
    private final ChangedConfigCommitPrecheck precheck = new ChangedConfigCommitPrecheck();

    @Test
    void proceedsWhenThereAreNoDeterministicFindings() {
        ChangedConfigCommitPrecheckResult result = precheck.evaluate(new ChangedConfigRiskAnalysis(List.of()));

        assertEquals(PROCEED, result.recommendation());
        assertEquals(0, result.findingCount());
        assertEquals(Optional.empty(), result.highestSeverity());
    }

    @Test
    void recommendsReviewWithoutBlockingWhenFindingsExist() {
        ChangedConfigCommitPrecheckResult result = precheck.evaluate(new ChangedConfigRiskAnalysis(List.of(
                finding("SCG004", Severity.WARNING, "logging.level.root"))));

        assertEquals(REVIEW_BEFORE_PROCEED, result.recommendation());
        assertEquals(1, result.findingCount());
        assertEquals(Optional.of(Severity.WARNING), result.highestSeverity());
        assertArrayEquals(new ChangedConfigCommitPrecheckResult.Recommendation[]{PROCEED, REVIEW_BEFORE_PROCEED},
                ChangedConfigCommitPrecheckResult.Recommendation.values());
    }

    @Test
    void highestSeverityIsIndependentOfFindingOrder() {
        ChangedConfigRiskFinding warning = finding("SCG004", Severity.WARNING, "logging.level.root");
        ChangedConfigRiskFinding critical = finding(
                "SCG001", Severity.CRITICAL, "spring.jpa.hibernate.ddl-auto");

        ChangedConfigCommitPrecheckResult first = precheck.evaluate(
                new ChangedConfigRiskAnalysis(List.of(warning, critical)));
        ChangedConfigCommitPrecheckResult second = precheck.evaluate(
                new ChangedConfigRiskAnalysis(List.of(critical, warning)));

        assertEquals(first, second);
        assertEquals(Optional.of(Severity.CRITICAL), first.highestSeverity());
    }

    @Test
    void rejectsInconsistentAggregateResults() {
        assertThrows(IllegalArgumentException.class,
                () -> new ChangedConfigCommitPrecheckResult(PROCEED, -1, Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> new ChangedConfigCommitPrecheckResult(PROCEED, 1, Optional.of(Severity.HIGH)));
        assertThrows(IllegalArgumentException.class,
                () -> new ChangedConfigCommitPrecheckResult(REVIEW_BEFORE_PROCEED, 0, Optional.empty()));
    }

    private static ChangedConfigRiskFinding finding(String ruleId, Severity severity, String key) {
        ConfigEntry entry = new ConfigEntry(key, "SENSITIVE_VALUE", "prod", "application-prod.yml", 4);
        ConfigChange change = new ConfigChange(ConfigChangeKind.MODIFIED,
                new ConfigEntry(key, "SAFE_VALUE", "prod", "application-prod.yml", 4), entry);
        return new ChangedConfigRiskFinding(change,
                new Finding(ruleId, severity, "message", "description", entry));
    }
}
