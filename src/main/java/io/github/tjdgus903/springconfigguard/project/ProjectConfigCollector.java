package io.github.tjdgus903.springconfigguard.project;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.scanner.ConfigProfileDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Thin IntelliJ adapter that collects candidate Spring Boot application configuration from project
 * content. Parsing and profile classification remain in the testable pure-Java source parser.
 */
public final class ProjectConfigCollector {
    private final ConfigProfileDetector profileDetector = new ConfigProfileDetector();
    private final ProjectConfigSourceParser sourceParser = new ProjectConfigSourceParser();

    /** Caller must hold read access. Current editor text takes precedence over saved content. */
    public List<ConfigEntry> collect(Project project) {
        List<VirtualFile> files = new ArrayList<>();

        ProjectFileIndex.getInstance(project).iterateContent(file -> {
            ProgressManager.checkCanceled();
            if (file.isDirectory() || profileDetector.detect(file.getName()).isEmpty()) {
                return true;
            }
            files.add(file);
            return true;
        });

        files.sort(Comparator.comparing(VirtualFile::getPath));
        List<ConfigEntry> entries = new ArrayList<>();
        for (VirtualFile file : files) {
            ProgressManager.checkCanceled();
            try {
                Document document = FileDocumentManager.getInstance().getCachedDocument(file);
                String content = document != null
                        ? document.getImmutableCharSequence().toString()
                        : VfsUtilCore.loadText(file);
                entries.addAll(sourceParser.parse(List.of(new ProjectConfigSource(file.getPath(), content))));
            } catch (IOException ignored) {
                // One unreadable configuration file must not abort project analysis.
            }
        }

        return List.copyOf(entries);
    }
}
