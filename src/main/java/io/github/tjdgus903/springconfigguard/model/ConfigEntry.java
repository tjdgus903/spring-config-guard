package io.github.tjdgus903.springconfigguard.model;

public record ConfigEntry(
        String key,
        String value,
        String profile,
        String filePath,
        int line
) {}
