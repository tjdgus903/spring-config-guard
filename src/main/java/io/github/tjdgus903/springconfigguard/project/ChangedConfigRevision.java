package io.github.tjdgus903.springconfigguard.project;

/**
 * Local before/after text for one changed path. A null side means that the revision is unavailable
 * (for example, an added or deleted file).
 */
public record ChangedConfigRevision(
        String beforePath,
        String beforeContent,
        String afterPath,
        String afterContent
) {}
