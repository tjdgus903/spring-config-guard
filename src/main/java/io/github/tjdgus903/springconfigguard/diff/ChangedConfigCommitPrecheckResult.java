package io.github.tjdgus903.springconfigguard.diff;

import io.github.tjdgus903.springconfigguard.model.Severity;

import java.util.Objects;
import java.util.Optional;

/** Value-free aggregate result that can recommend review but cannot reject a commit. */
public record ChangedConfigCommitPrecheckResult(
        Recommendation recommendation,
        int findingCount,
        Optional<Severity> highestSeverity
) {
    public ChangedConfigCommitPrecheckResult {
        Objects.requireNonNull(recommendation, "recommendation");
        Objects.requireNonNull(highestSeverity, "highestSeverity");
        if (findingCount < 0) {
            throw new IllegalArgumentException("findingCount must not be negative");
        }
        if (findingCount == 0 && (recommendation != Recommendation.PROCEED || highestSeverity.isPresent())) {
            throw new IllegalArgumentException("A result without findings must proceed without a severity");
        }
        if (findingCount > 0
                && (recommendation != Recommendation.REVIEW_BEFORE_PROCEED || highestSeverity.isEmpty())) {
            throw new IllegalArgumentException("A result with findings must recommend review with a severity");
        }
    }

    public static ChangedConfigCommitPrecheckResult proceed() {
        return new ChangedConfigCommitPrecheckResult(Recommendation.PROCEED, 0, Optional.empty());
    }

    public static ChangedConfigCommitPrecheckResult reviewBeforeProceed(int findingCount, Severity highestSeverity) {
        return new ChangedConfigCommitPrecheckResult(
                Recommendation.REVIEW_BEFORE_PROCEED,
                findingCount,
                Optional.of(Objects.requireNonNull(highestSeverity, "highestSeverity")));
    }

    /** Intentionally contains no reject or block state. */
    public enum Recommendation {
        PROCEED,
        REVIEW_BEFORE_PROCEED
    }
}
