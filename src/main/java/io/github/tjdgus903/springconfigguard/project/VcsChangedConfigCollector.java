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
import io.github.tjdgus903.springconfigguard.scanner.ConfigProfileDetector;
import io.github.tjdgus903.springconfigguard.settings.SpringConfigGuardSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Thin local IntelliJ VCS adapter. Project-wide analysis reads the current change list and
 * supported unversioned project files, preferring unsaved cached editor text for a tracked
 * after-side only while its canonical file stays inside the project. Selected-change analysis remains
 * limited to its supplied revisions. It never contacts
 * a remote VCS service or sends project content outside the IDE process. Project-relative paths are
 * validated before content is read.
 */
public final class VcsChangedConfigCollector {
    private ChangedConfigRevisionParser parser(Project project) {
        return new ChangedConfigRevisionParser(new ConfigProfileDetector(
                SpringConfigGuardSettings.getInstance(project).getProductionAliases()));
    }

    /** Caller must hold read access. */
    public ChangedConfigEntries collect(Project project) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        return collect(project, changeListManager.getAllChanges(),
                changeListManager.getUnversionedFilesPaths(), true);
    }

    /** Caller must hold read access. Only the supplied local changes are read. */
    public ChangedConfigEntries collect(Project project, Collection<? extends Change> selectedChanges) {
        return collect(project, selectedChanges, List.of(), false);
    }

    private ChangedConfigEntries collect(Project project, Collection<? extends Change> selectedChanges,
                                         Collection<? extends FilePath> unversionedPaths,
                                         boolean preferCurrentDocuments) {
        ChangedConfigRevisionParser parser = parser(project);
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
            if (!isSpringConfigChange(parser, beforePath, afterPath)) {
                continue;
            }
            revisions.add(new ChangedConfigRevision(
                    beforePath,
                    contentOfSpringConfig(parser, beforePath, before),
                    afterPath,
                    contentOfSpringConfig(
                            parser,
                            afterPath,
                            after,
                            () -> cachedDocumentContent(project, after),
                            preferCurrentDocuments
                    )
            ));
        }
        for (FilePath filePath : unversionedFiles) {
            ProgressManager.checkCanceled();
            if (filePath.isDirectory()) {
                continue;
            }
            ChangedConfigRevision revision = unversionedRevision(
                    parser, project.getBasePath(),
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
        return unversionedRevision(new ChangedConfigRevisionParser(), basePath, absolutePath, contentSupplier);
    }

    private ChangedConfigRevision unversionedRevision(ChangedConfigRevisionParser parser, String basePath, String absolutePath,
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
        return isSpringConfigChange(new ChangedConfigRevisionParser(), beforePath, afterPath);
    }

    boolean isSpringConfigChange(ChangedConfigRevisionParser parser, String beforePath, String afterPath) {
        return parser.isSpringConfigPath(beforePath) || parser.isSpringConfigPath(afterPath);
    }

    String contentOfSpringConfig(String path, ContentRevision revision) {
        return contentOfSpringConfig(new ChangedConfigRevisionParser(), path, revision);
    }

    String contentOfSpringConfig(ChangedConfigRevisionParser parser, String path, ContentRevision revision) {
        return contentOfSpringConfig(parser, path, revision, () -> null, false);
    }

    String contentOfSpringConfig(String path, ContentRevision revision,
                                 Supplier<String> currentDocumentSupplier,
                                 boolean preferCurrentDocument) {
        return contentOfSpringConfig(
                new ChangedConfigRevisionParser(),
                path,
                revision,
                currentDocumentSupplier,
                preferCurrentDocument
        );
    }

    String contentOfSpringConfig(ChangedConfigRevisionParser parser, String path, ContentRevision revision,
                                 Supplier<String> currentDocumentSupplier,
                                 boolean preferCurrentDocument) {
        if (!parser.isSpringConfigPath(path)) {
            return null;
        }
        if (revision == null) {
            return null;
        }
        if (preferCurrentDocument) {
            String currentDocument = currentDocumentSupplier.get();
            if (currentDocument != null) {
                return currentDocument;
            }
        }
        return contentOf(revision);
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

    private static String cachedDocumentContent(Project project, ContentRevision revision) {
        if (revision == null) {
            return null;
        }
        VirtualFile baseDirectory = project.getBaseDir();
        VirtualFile file = revision.getFile().getVirtualFile();
        VirtualFile canonicalBase = baseDirectory == null ? null : baseDirectory.getCanonicalFile();
        VirtualFile canonicalFile = file == null ? null : file.getCanonicalFile();
        if (file == null || file.isDirectory()
                || !isCanonicalProjectPath(
                        canonicalBase == null ? null : canonicalBase.getPath(),
                        canonicalFile == null ? null : canonicalFile.getPath())) {
            return null;
        }
        Document cachedDocument = FileDocumentManager.getInstance().getCachedDocument(file);
        return cachedDocument == null ? null : cachedDocument.getText();
    }

    static boolean isCanonicalProjectPath(String canonicalBasePath, String canonicalPath) {
        return projectRelativePath(canonicalBasePath, canonicalPath) != null;
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
