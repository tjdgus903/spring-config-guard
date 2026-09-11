package io.github.tjdgus903.springconfigguard.drift;

import java.util.List;
import java.util.Optional;

/** Immutable output of deterministic profile drift analysis. */
public record ProfileDriftAnalysis(
        List<EffectiveConfigValue> effectiveValues,
        List<ProfileDriftFinding> findings
) {
    public ProfileDriftAnalysis {
        effectiveValues = List.copyOf(effectiveValues);
        findings = List.copyOf(findings);
    }

    public Optional<EffectiveConfigValue> effectiveValue(String profile, String key) {
        return effectiveValues.stream()
                .filter(value -> value.profile().equalsIgnoreCase(profile) && value.key().equals(key))
                .findFirst();
    }
}
