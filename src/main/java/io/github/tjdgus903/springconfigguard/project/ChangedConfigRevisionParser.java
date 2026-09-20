package io.github.tjdgus903.springconfigguard.project;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.scanner.ConfigProfileDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure parser for VCS revision text. It has no IntelliJ dependency. A genuinely absent revision
 * side has neither path nor content and becomes an empty entry inventory so additions and removals
 * reach the diff core; every supported path must include content and every list element must be
 * a complete revision record.
 */
public final class ChangedConfigRevisionParser {
    private final ConfigProfileDetector profileDetector = new ConfigProfileDetector();
    private final ProjectConfigSourceParser sourceParser = new ProjectConfigSourceParser();

    public ChangedConfigEntries parse(List<ChangedConfigRevision> revisions) {
        Objects.requireNonNull(revisions, "revisions");

        List<ProjectConfigSource> beforeSources = new ArrayList<>();
        List<ProjectConfigSource> afterSources = new ArrayList<>();
        for (ChangedConfigRevision revision : revisions) {
            if (revision == null) {
                throw new IllegalStateException("Could not read a local VCS revision.");
            }

            requirePathForContent(revision.beforePath(), revision.beforeContent());
            requirePathForContent(revision.afterPath(), revision.afterContent());

            boolean beforeIsConfig = isSpringConfigPath(revision.beforePath());
            boolean afterIsConfig = isSpringConfigPath(revision.afterPath());
            if (!beforeIsConfig && !afterIsConfig) {
                continue;
            }

            if (beforeIsConfig) {
                beforeSources.add(requiredSource(revision.beforePath(), revision.beforeContent()));
            }
            if (afterIsConfig) {
                afterSources.add(requiredSource(revision.afterPath(), revision.afterContent()));
            }
        }

        List<ConfigEntry> before = sourceParser.parseStrict(beforeSources);
        List<ConfigEntry> after = sourceParser.parseStrict(afterSources);
        return new ChangedConfigEntries(before, after);
    }

    private static void requirePathForContent(String path, String content) {
        if (path == null && content != null) {
            throw new IllegalStateException("Could not read a local VCS revision.");
        }
    }

    private static ProjectConfigSource requiredSource(String path, String content) {
        if (content == null) {
            throw new IllegalStateException("Could not read a local VCS revision.");
        }
        return new ProjectConfigSource(path, content);
    }

    public boolean isSpringConfigPath(String path) {
        return profileDetector.detect(path).isPresent();
    }
}
