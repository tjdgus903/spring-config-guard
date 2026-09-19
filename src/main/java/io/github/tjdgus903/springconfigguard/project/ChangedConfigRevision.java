package io.github.tjdgus903.springconfigguard.project;

/**
 * Local before/after snapshots for one VCS change. An absent side must have both path and content
 * null (for example, before an addition or after a deletion); a supported path requires content.
 */
public record ChangedConfigRevision(
        String beforePath,
        String beforeContent,
        String afterPath,
        String afterContent
) {}
