package io.github.tjdgus903.springconfigguard.code;

/** One configuration-property binding target discovered from Java source. */
public record ConfigurationPropertyMapping(
        String key,
        String prefix,
        String declaringClass,
        String fieldName,
        String filePath,
        int line
) {
}
