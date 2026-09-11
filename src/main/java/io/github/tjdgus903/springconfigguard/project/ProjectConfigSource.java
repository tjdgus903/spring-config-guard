package io.github.tjdgus903.springconfigguard.project;

/** Raw local configuration source before deterministic Spring parsing. */
public record ProjectConfigSource(
        String path,
        String content
) {}
