package io.github.tjdgus903.springconfigguard.project;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.scanner.ConfigProfileDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure parser for VCS revision text. It has no IntelliJ dependency and deliberately treats an
 * unavailable revision as an empty entry inventory so additions and removals reach the diff core.
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
                continue;
            }

            boolean beforeIsConfig = isSpringConfigPath(revision.beforePath());
            boolean afterIsConfig = isSpringConfigPath(revision.afterPath());
            if (!beforeIsConfig && !afterIsConfig) {
                continue;
            }

            // Prefer the current config path; deletion and rename-out use the prior config path.
            String sharedPath = afterIsConfig ? revision.afterPath() : revision.beforePath();
            if (beforeIsConfig && revision.beforeContent() != null) {
                beforeSources.add(new ProjectConfigSource(sharedPath, revision.beforeContent()));
            }
            if (afterIsConfig && revision.afterContent() != null) {
                afterSources.add(new ProjectConfigSource(sharedPath, revision.afterContent()));
            }
        }

        List<ConfigEntry> before = sourceParser.parse(beforeSources);
        List<ConfigEntry> after = sourceParser.parse(afterSources);
        return new ChangedConfigEntries(before, after);
    }

    public boolean isSpringConfigPath(String path) {
        return profileDetector.detect(path).isPresent();
    }
}
