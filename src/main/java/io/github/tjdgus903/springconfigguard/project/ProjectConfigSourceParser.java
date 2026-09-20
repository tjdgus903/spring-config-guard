package io.github.tjdgus903.springconfigguard.project;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.scanner.ConfigFileScanner;
import io.github.tjdgus903.springconfigguard.scanner.ConfigProfile;
import io.github.tjdgus903.springconfigguard.scanner.ConfigProfileDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure parser for raw project configuration sources. It deliberately has no IntelliJ dependency so
 * source selection/parsing behavior can be tested without initializing unrelated IDE services.
 */
public final class ProjectConfigSourceParser {
    private final ConfigProfileDetector profileDetector;
    private final ConfigFileScanner scanner = new ConfigFileScanner();

    public ProjectConfigSourceParser() { this(new ConfigProfileDetector()); }
    public ProjectConfigSourceParser(ConfigProfileDetector profileDetector) { this.profileDetector = profileDetector; }

    public List<ConfigEntry> parse(List<ProjectConfigSource> sources) {
        return parse(sources, false);
    }

    List<ConfigEntry> parseStrict(List<ProjectConfigSource> sources) {
        return parse(sources, true);
    }

    private List<ConfigEntry> parse(List<ProjectConfigSource> sources, boolean failOnMalformed) {
        List<ConfigEntry> entries = new ArrayList<>();

        for (ProjectConfigSource source : sources) {
            if (source == null || source.path() == null || source.content() == null) {
                continue;
            }

            Optional<ConfigProfile> profile = profileDetector.detect(source.path());
            if (profile.isEmpty()) {
                continue;
            }

            try {
                entries.addAll(scanner.scan(source.content(), source.path(), profile.get().name()));
            } catch (RuntimeException ignored) {
                if (failOnMalformed) {
                    throw new IllegalStateException("Could not parse local configuration.");
                }
                // Tolerant project scans skip malformed sources independently.
            }
        }

        return List.copyOf(entries);
    }
}
