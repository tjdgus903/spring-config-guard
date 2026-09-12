package io.github.tjdgus903.springconfigguard.diff;

import io.github.tjdgus903.springconfigguard.model.Severity;

import java.util.Comparator;
import java.util.Objects;

/** Produces a deterministic, non-blocking recommendation for a future local commit adapter. */
public final class ChangedConfigCommitPrecheck {

    public ChangedConfigCommitPrecheckResult evaluate(ChangedConfigRiskAnalysis riskAnalysis) {
        Objects.requireNonNull(riskAnalysis, "riskAnalysis");

        if (riskAnalysis.findings().isEmpty()) {
            return ChangedConfigCommitPrecheckResult.proceed();
        }

        Severity highestSeverity = riskAnalysis.findings().stream()
                .map(item -> item.finding().severity())
                .max(Comparator.naturalOrder())
                .orElseThrow();
        return ChangedConfigCommitPrecheckResult.reviewBeforeProceed(
                riskAnalysis.findings().size(), highestSeverity);
    }
}
