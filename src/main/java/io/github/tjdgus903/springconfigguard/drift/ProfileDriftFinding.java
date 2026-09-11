package io.github.tjdgus903.springconfigguard.drift;

import io.github.tjdgus903.springconfigguard.model.Severity;

/**
 * One profile drift observation. `ruleId` is null for an informational plain difference and is
 * populated for deterministic risk rules.
 */
public record ProfileDriftFinding(
        String ruleId,
        ProfileDriftKind kind,
        Severity severity,
        String key,
        String profile,
        String message,
        String description,
        EffectiveConfigValue effectiveValue
) {}
