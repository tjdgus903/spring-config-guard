package io.github.tjdgus903.springconfigguard.project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.scanner.ConfigProfileDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin IntelliJ adapter that collects candidate Spring Boot application configuration from project
 * content. Parsing and profile classification remain in the testable pure-Java source parser.
 */
public final class ProjectConfigCollector {
    private final ConfigProfileDetector profileDetector = new ConfigProfileDetector();
    private final ProjectConfigSourceParser sourceParser = new ProjectConfigSourceParser();

    public List<ConfigEntry> collect(Project project) {
        List<ProjectConfigSource> sources = new ArrayList<>();

        ProjectFileIndex.getInstance(project).iterateContent(file -> {
            if (file.isDirectory() || profileDetector.detect(file.getName()).isEmpty()) {
                return true;
            }

            try {
                sources.add(new ProjectConfigSource(file.getPath(), VfsUtilCore.loadText(file)));
            } catch (IOException ignored) {
                // One unreadable configuration file must not abort project analysis.
            }
            return true;
        });

        return sourceParser.parse(sources);
    }
}
