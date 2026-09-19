package io.github.tjdgus903.springconfigguard.project;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Thin local IntelliJ VCS adapter. It reads only the current change list and never contacts a
 * remote VCS service or sends project content outside the IDE process.
 */
public final class VcsChangedConfigCollector {
    private final ChangedConfigRevisionParser parser = new ChangedConfigRevisionParser();

    /** Caller must hold read access. */
    public ChangedConfigEntries collect(Project project) {
        return collect(project, ChangeListManager.getInstance(project).getAllChanges());
    }

    /** Caller must hold read access. Only the supplied local changes are read. */
    public ChangedConfigEntries collect(Project project, Collection<? extends Change> selectedChanges) {
        List<Change> changes = new ArrayList<>(selectedChanges);
        changes.sort(Comparator.comparing(this::sortKey));

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
        return parser.parse(revisions);
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
        String path = revision.getFile().getPath();
        String basePath = project.getBasePath();
        if (basePath == null) {
            return path.replace('\\', '/');
        }
        String relative = FileUtil.getRelativePath(basePath, path, '/');
        return relative == null ? path.replace('\\', '/') : relative;
    }

    static String contentOf(ContentRevision revision) {
        if (revision == null) {
            return null;
        }
        try {
            String content = revision.getContent();
            if (content == null) {
                throw new IllegalStateException("Could not read a local VCS revision.");
            }
            return content;
        } catch (VcsException ignored) {
            throw new IllegalStateException("Could not read a local VCS revision.");
        }
    }
}
