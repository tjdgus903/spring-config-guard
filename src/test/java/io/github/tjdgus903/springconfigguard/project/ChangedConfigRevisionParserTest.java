package io.github.tjdgus903.springconfigguard.project;

import io.github.tjdgus903.springconfigguard.diff.ConfigChangeKind;
import io.github.tjdgus903.springconfigguard.diff.ConfigEntryDiffAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangedConfigRevisionParserTest {
    private final ChangedConfigRevisionParser parser = new ChangedConfigRevisionParser();

    @Test
    void parsesBothRevisionsWithTheSameProjectRelativePath() {
        ChangedConfigEntries entries = parser.parse(List.of(new ChangedConfigRevision(
                "src/main/resources/application-prod.yml",
                "spring:\\n  jpa:\\n    show-sql: false\\n",
                "src/main/resources/application-prod.yml",
                "spring:\\n  jpa:\\n    show-sql: true\\n"
        )));

        assertEquals("src/main/resources/application-prod.yml", entries.before().getFirst().filePath());
        assertEquals("src/main/resources/application-prod.yml", entries.after().getFirst().filePath());
        assertEquals("prod", entries.after().getFirst().profile());
        assertEquals(List.of(ConfigChangeKind.MODIFIED), new ConfigEntryDiffAnalyzer().analyze(
                entries.before(), entries.after()).changes().stream().map(change -> change.kind()).toList());
    }

    @Test
    void preservesRevisionPathsAndProfilesAcrossSupportedRename() {
        ChangedConfigEntries entries = parser.parse(List.of(new ChangedConfigRevision(
                "src/main/resources/application-dev.properties",
                "feature.enabled=true",
                "src/main/resources/application-prod.properties",
                "feature.enabled=true"
        )));

        assertEquals("src/main/resources/application-dev.properties",
                entries.before().getFirst().filePath());
        assertEquals("dev", entries.before().getFirst().profile());
        assertEquals("src/main/resources/application-prod.properties",
                entries.after().getFirst().filePath());
        assertEquals("prod", entries.after().getFirst().profile());
        assertEquals(List.of(ConfigChangeKind.REMOVED, ConfigChangeKind.ADDED),
                new ConfigEntryDiffAnalyzer().analyze(entries.before(), entries.after()).changes().stream()
                        .map(change -> change.kind())
                        .toList());
    }

    @Test
    void incompleteSupportedRevisionStopsAnalysisWithoutDeletionLikeOutput() {
        IllegalStateException beforeError = assertThrows(
                IllegalStateException.class,
                () -> parser.parse(List.of(new ChangedConfigRevision(
                        "src/main/resources/application-prod.yml",
                        null,
                        "src/main/resources/application-prod.yml",
                        "feature.enabled=true"
                )))
        );
        assertEquals("Could not read a local VCS revision.", beforeError.getMessage());
        assertNull(beforeError.getCause());

        IllegalStateException afterError = assertThrows(
                IllegalStateException.class,
                () -> parser.parse(List.of(new ChangedConfigRevision(
                        "src/main/resources/application-prod.yml",
                        "feature.enabled=true",
                        "src/main/resources/application-prod.yml",
                        null
                )))
        );
        assertEquals("Could not read a local VCS revision.", afterError.getMessage());
        assertNull(afterError.getCause());
    }

    @Test
    void malformedSupportedRevisionStopsAnalysisWithoutExposingContent() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> parser.parse(List.of(new ChangedConfigRevision(
                        null,
                        null,
                        "src/main/resources/application-prod.yml",
                        "spring: [PRIVATE_CONFIG_VALUE"
                ))));

        assertEquals("Could not parse local configuration.", error.getMessage());
        assertNull(error.getCause());
        assertFalse(error.getMessage().contains("PRIVATE_CONFIG_VALUE"));
    }

    @Test
    void ignoresNonSpringConfigurationPaths() {
        ChangedConfigEntries entries = parser.parse(List.of(
                new ChangedConfigRevision("README.md", "app.enabled=false", "README.md", "app.enabled=true"),
                new ChangedConfigRevision("src/service.yaml", "app.enabled=false", "src/service.yaml", "app.enabled=true")
        ));

        assertTrue(entries.before().isEmpty());
        assertTrue(entries.after().isEmpty());
    }

    @Test
    void representsAddedAndDeletedConfigFilesWithAnEmptyOppositeSide() {
        ChangedConfigEntries added = parser.parse(List.of(new ChangedConfigRevision(
                null, null, "src/main/resources/application.properties", "feature.enabled=true"
        )));
        ChangedConfigEntries removed = parser.parse(List.of(new ChangedConfigRevision(
                "src/main/resources/application-prod.properties", "feature.enabled=true", null, null
        )));

        assertTrue(added.before().isEmpty());
        assertEquals(1, added.after().size());
        assertEquals(1, removed.before().size());
        assertTrue(removed.after().isEmpty());
    }
}
