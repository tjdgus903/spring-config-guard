package io.github.tjdgus903.springconfigguard.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangedConfigReportLocationTest {
    @Test
    void parsesProjectRelativeFindingLocation() {
        var location = ChangedConfigReportLocation.parse(
                "- [HIGH] [SCG002] management.endpoints.web.exposure.include (profile: prod) at src/main/resources/application-prod.yml:12"
        ).orElseThrow();

        assertEquals("src/main/resources/application-prod.yml", location.path());
        assertEquals(12, location.line());
    }

    @Test
    void parsesWindowsAbsoluteFindingLocationUsingLastColon() {
        var location = ChangedConfigReportLocation.parse(
                "- [WARNING] [SCG004] logging.level.root (profile: prod) at C:/work/app/application-prod.yml:7"
        ).orElseThrow();

        assertEquals("C:/work/app/application-prod.yml", location.path());
        assertEquals(7, location.line());
    }

    @Test
    void ignoresNonFindingAndInvalidLocationLines() {
        assertTrue(ChangedConfigReportLocation.parse("Changed entries: 3").isEmpty());
        assertTrue(ChangedConfigReportLocation.parse("- finding at application.yml:0").isEmpty());
        assertTrue(ChangedConfigReportLocation.parse(null).isEmpty());
    }
}
