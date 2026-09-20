package io.github.tjdgus903.springconfigguard.project;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VcsChangedConfigCollectorTest {
    private final VcsChangedConfigCollector collector = new VcsChangedConfigCollector();

    @Test
    void keepsOnlyPathsInsideTheProjectBoundary() {
        assertEquals("src/main/resources/application-prod.yml",
                VcsChangedConfigCollector.projectRelativePath(
                        "/workspace/project",
                        "/workspace/project/src/main/resources/application-prod.yml"));
        assertNull(VcsChangedConfigCollector.projectRelativePath(
                "/workspace/project",
                "/workspace/other/application-prod.yml"));
        assertNull(VcsChangedConfigCollector.projectRelativePath(
                "/workspace/project",
                "/workspace/project/src/../../outside/application-prod.yml"));
        assertNull(VcsChangedConfigCollector.projectRelativePath(
                "C:\\workspace\\project",
                "C:\\workspace\\project\\src\\..\\..\\outside\\application-prod.yml"));
        assertNull(VcsChangedConfigCollector.projectRelativePath(
                null,
                "/workspace/project/src/main/resources/application-prod.yml"));
    }

    @Test
    void distinguishesMissingReadableAndUnavailableRevisionsWithoutExposingErrors() {
        assertNull(VcsChangedConfigCollector.contentOf(null));
        assertEquals("server.port=8080",
                VcsChangedConfigCollector.contentOf(revision("server.port=8080", false)));

        IllegalStateException missingContent = assertThrows(IllegalStateException.class,
                () -> VcsChangedConfigCollector.contentOf(revision(null, false)));
        assertEquals("Could not read a local VCS revision.", missingContent.getMessage());
        assertNull(missingContent.getCause());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> VcsChangedConfigCollector.contentOf(revision(null, true)));
        assertEquals("Could not read a local VCS revision.", error.getMessage());
        assertNull(error.getCause());
        assertFalse(error.getMessage().contains("PRIVATE_VCS_ERROR"));
    }

    @Test
    void convertsSupportedUnversionedProjectFileToAnAdditionAfterPathFiltering() {
        AtomicInteger reads = new AtomicInteger();

        ChangedConfigRevision revision = collector.unversionedRevision(
                "/workspace/project",
                "/workspace/project/src/main/resources/application-prod.yml",
                () -> {
                    reads.incrementAndGet();
                    return "spring.jpa.show-sql=true";
                }
        );

        assertNull(revision.beforePath());
        assertNull(revision.beforeContent());
        assertEquals("src/main/resources/application-prod.yml", revision.afterPath());
        assertEquals("spring.jpa.show-sql=true", revision.afterContent());
        assertEquals(1, reads.get());
    }

    @Test
    void rejectsUnrelatedAndOutsideUnversionedPathsBeforeReadingContent() {
        assertNull(collector.unversionedRevision(
                "/workspace/project",
                "/workspace/project/README.md",
                VcsChangedConfigCollectorTest::unexpectedContentRead
        ));
        assertNull(collector.unversionedRevision(
                "/workspace/project",
                "/workspace/other/application-prod.yml",
                VcsChangedConfigCollectorTest::unexpectedContentRead
        ));
        assertNull(collector.unversionedRevision(
                "/workspace/project",
                "/workspace/project/src/../../outside/application-prod.yml",
                VcsChangedConfigCollectorTest::unexpectedContentRead
        ));
    }

    @Test
    void missingUnversionedContentStopsAnalysisWithValueFreeError() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> collector.unversionedRevision(
                        "/workspace/project",
                        "/workspace/project/application-prod.yml",
                        () -> null
                ));

        assertEquals("Could not read a local VCS revision.", error.getMessage());
        assertNull(error.getCause());
    }

    @Test
    void readsOnlyTheSupportedSideOfRenamedConfigurationChanges() {
        assertNull(collector.contentOfSpringConfig(
                "config/legacy.yml", revision(null, true)));
        assertEquals("server.port=8080", collector.contentOfSpringConfig(
                "src/main/resources/application-prod.yml",
                revision("server.port=8080", false)));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> collector.contentOfSpringConfig(
                        "src/main/resources/application-prod.yml",
                        revision(null, true)));
        assertEquals("Could not read a local VCS revision.", error.getMessage());
        assertNull(error.getCause());
    }

    @Test
    void selectsSupportedSpringConfigurationChanges() {
        assertTrue(collector.isSpringConfigChange(
                "src/main/resources/application-prod.yml",
                "src/main/resources/application-prod.yml"));
        assertTrue(collector.isSpringConfigChange(
                null,
                "config/application.properties"));
        assertTrue(collector.isSpringConfigChange(
                "config/application-local.yaml",
                null));
    }

    @Test
    void rejectsUnrelatedChangedFilePathsBeforeContentIsRead() {
        assertFalse(collector.isSpringConfigChange(
                "src/main/java/example/App.java",
                "src/main/java/example/App.java"));
        assertFalse(collector.isSpringConfigChange(
                "README.md",
                "docs/README.md"));
        assertFalse(collector.isSpringConfigChange(
                "deploy/service.yaml",
                "deploy/service.yaml"));
    }

    @Test
    void retainsRenamesIntoAndOutOfSupportedConfigurationPaths() {
        assertTrue(collector.isSpringConfigChange(
                "config/legacy.yml",
                "src/main/resources/application-prod.yml"));
        assertTrue(collector.isSpringConfigChange(
                "src/main/resources/application-prod.yml",
                "config/archived.yml"));
    }

    private static String unexpectedContentRead() {
        throw new AssertionError("content must not be read");
    }

    private static ContentRevision revision(String content, boolean fails) {
        return new ContentRevision() {
            @Override
            public String getContent() throws VcsException {
                if (fails) throw new VcsException("PRIVATE_VCS_ERROR");
                return content;
            }

            @Override
            public FilePath getFile() {
                throw new AssertionError("getFile must not be called by contentOf");
            }

            @Override
            public VcsRevisionNumber getRevisionNumber() {
                throw new AssertionError("getRevisionNumber must not be called by contentOf");
            }
        };
    }
}
