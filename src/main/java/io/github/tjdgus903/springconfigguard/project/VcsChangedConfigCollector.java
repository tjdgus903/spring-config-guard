package io.github.tjdgus903.springconfigguard.project;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Thin local IntelliJ VCS adapter. It reads only the current change list and never contacts a
 * remote VCS service or sends project content outside the IDE process. Project-relative paths are
 * validated before revision content is read.
 */
public final class VcsChangedConfigCollector {
    private final ChangedConfigRevisionParser parser = new ChangedConfigRevisionParser();

    /** Caller must hold read access. */
    public ChangedConfigEntries collect(Project project) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        return collect(project, changeListManager.getAllChanges(),
                changeListManager.getUnversionedFilesPaths());
    }

    /** Caller must hold read access. Only the supplied local changes are read. */
    public ChangedConfigEntries collect(Project project, Collection<? extends Change> selectedChanges) {
        return collect(project, selectedChanges, List.of());
    }

    private ChangedConfigEntries collect(Project project, Collection<? extends Change> selectedChanges,
                                         Collection<? extends FilePath> unversionedPaths) {
        List<Change> changes = new ArrayList<>(selectedChanges);
        changes.sort(Comparator.comparing(this::sortKey));
        List<FilePath> unversionedFiles = new ArrayList<>(unversionedPaths);
        unversionedFiles.sort(Comparator.comparing(FilePath::getPath));

        List<ChangedConfigRevision> revisions = new ArrayList<>();
        for (Change change : changes) {
            ProgressManager.checkCanceled();
            ContentRevision before = change.getBeforeRevision();
            ContentRevision after = change.getAfterRevision();
            String beforePath = relativePath(project, before);
            String afterPath = relativePath(project, after);
            if (!isSpringConfigChange(beforePath, afterPath)) {
                continue;
            }
            revisions.add(new ChangedConfigRevision(
                    beforePath,
                    contentOfSpringConfig(beforePath, before),
                    afterPath,
                    contentOfSpringConfig(afterPath, after)
            ));
        }
        for (FilePath filePath : unversionedFiles) {
            ProgressManager.checkCanceled();
            if (filePath.isDirectory()) {
                continue;
            }
            ChangedConfigRevision revision = unversionedRevision(
                    project.getBasePath(),
                    filePath.getPath(),
                    () -> contentOfUnversionedFile(project, filePath)
            );
            if (revision != null) {
                revisions.add(revision);
            }
        }
        return parser.parse(revisions);
    }

    ChangedConfigRevision unversionedRevision(String basePath, String absolutePath,
                                              Supplier<String> contentSupplier) {
        String relativePath = projectRelativePath(basePath, absolutePath);
        if (!parser.isSpringConfigPath(relativePath)) {
            return null;
        }
        String content = contentSupplier.get();
        if (content == null) {
            throw unreadableRevision();
        }
        return new ChangedConfigRevision(null, null, relativePath, content);
    }

    boolean isSpringConfigChange(String beforePath, String afterPath) {
        return parser.isSpringConfigPath(beforePath) || parser.isSpringConfigPath(afterPath);
    }

    String contentOfSpringConfig(String path, ContentRevision revision) {
        return parser.isSpringConfigPath(path) ? contentOf(revision) : null;
    }

    private String sortKey(Change change) {
        ContentRevision after = change.getAfterRevision();
        ContentRevision before = change.getBeforeRevision();
        if (after != null) {
            return after.getFile().getPath();
        }
        return before == null ? "" : before.getFile().getPath();
    }

    private static String relativePath(Project project, ContentRevision revision) {
        if (revision == null) {
            return null;
        }
        return projectRelativePath(project.getBasePath(), revision.getFile().getPath());
    }

    static String projectRelativePath(String basePath, String path) {
        if (basePath == null || path == null
                || hasParentTraversal(basePath) || hasParentTraversal(path)) {
            return null;
        }
        String relative = FileUtil.getRelativePath(basePath, path, '/');
        if (relative == null || hasParentTraversal(relative)) {
            return null;
        }
        return relative;
    }

    private static boolean hasParentTraversal(String path) {
        String separated = path.replace('\\', '/');
        return separated.equals("..")
                || separated.startsWith("../")
                || separated.endsWith("/..")
                || separated.contains("/../");
    }

    private static String contentOfUnversionedFile(Project project, FilePath filePath) {
        VirtualFile baseDirectory = project.getBaseDir();
        VirtualFile file = filePath.getVirtualFile();
        VirtualFile canonicalBase = baseDirectory == null ? null : baseDirectory.getCanonicalFile();
        VirtualFile canonicalFile = file == null ? null : file.getCanonicalFile();
        if (file == null || file.isDirectory() || canonicalBase == null || canonicalFile == null
                || !VfsUtilCore.isAncestor(canonicalBase, canonicalFile, false)) {
            throw unreadableRevision();
        }

        Document cachedDocument = FileDocumentManager.getInstance().getCachedDocument(file);
        if (cachedDocument != null) {
            return cachedDocument.getText();
        }
        try {
            return VfsUtilCore.loadText(file);
        } catch (IOException ignored) {
            throw unreadableRevision();
        }
    }

    static String contentOf(ContentRevision revision) {
        if (revision == null) {
            return null;
        }
        try {
            String content = revision.getContent();
            if (content == null) {
                throw unreadableRevision();
            }
            return content;
        } catch (VcsException ignored) {
            throw unreadableRevision();
        }
    }

    private static IllegalStateException unreadableRevision() {
        return new IllegalStateException("Could not read a local VCS revision.");
    }
}
