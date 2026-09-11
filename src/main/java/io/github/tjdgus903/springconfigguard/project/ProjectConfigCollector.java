package io.github.tjdgus903.springconfigguard.project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.scanner.ConfigFileScanner;
import io.github.tjdgus903.springconfigguard.scanner.ConfigProfile;
import io.github.tjdgus903.springconfigguard.scanner.ConfigProfileDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Collects Spring Boot application configuration from project content only. */
public final class ProjectConfigCollector {
    private final ConfigProfileDetector profileDetector = new ConfigProfileDetector();
    private final ConfigFileScanner scanner = new ConfigFileScanner();

    public List<ConfigEntry> collect(Project project) {
        List<ConfigEntry> entries = new ArrayList<>();

        ProjectFileIndex.getInstance(project).iterateContent(file -> {
            if (file.isDirectory()) {
                return true;
            }

            Optional<ConfigProfile> profile = profileDetector.detect(file.getName());
            if (profile.isEmpty()) {
                return true;
            }

            try {
                String content = VfsUtilCore.loadText(file);
                entries.addAll(scanner.scan(content, file.getPath(), profile.get().name()));
            } catch (IOException | RuntimeException ignored) {
                // One unreadable or malformed configuration file must not abort project analysis.
            }
            return true;
        });

        return List.copyOf(entries);
    }
}
