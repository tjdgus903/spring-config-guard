package io.github.tjdgus903.springconfigguard.model;

public record Finding(
        String ruleId,
        Severity severity,
        String message,
        String description,
        ConfigEntry entry
) {}
