package io.github.tjdgus903.springconfigguard.project;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VcsChangedConfigCollectorTest {
    private final VcsChangedConfigCollector collector = new VcsChangedConfigCollector();

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
}
