package io.github.tjdgus903.springconfigguard.drift;

import java.util.Optional;

/** Deterministic rule applied to one effective profile configuration value. */
public interface ProfileDriftRule {
    String id();

    Optional<ProfileDriftFinding> check(EffectiveConfigValue value, boolean productionProfile);
}
