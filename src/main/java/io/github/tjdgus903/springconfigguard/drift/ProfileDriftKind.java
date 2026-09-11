package io.github.tjdgus903.springconfigguard.drift;

/** Separates ordinary configuration differences from deterministic risk findings. */
public enum ProfileDriftKind {
    DIFFERENCE,
    RISK
}
