package io.github.tjdgus903.springconfigguard.code;

/** One configuration key reference discovered from source code. */
public record ConfigUsage(
        String key,
        String defaultValue,
        String expression,
        String filePath,
        int line
) {
}
