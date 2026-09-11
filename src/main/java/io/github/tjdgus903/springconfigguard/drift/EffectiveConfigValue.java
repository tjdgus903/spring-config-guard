package io.github.tjdgus903.springconfigguard.drift;

/**
 * Effective value of a configuration key for one target profile after applying the default
 * application configuration as the fallback.
 */
public record EffectiveConfigValue(
        String key,
        String profile,
        String value,
        boolean inherited,
        String sourceProfile,
        String sourceFilePath,
        int sourceLine
) {}
