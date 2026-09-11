package io.github.tjdgus903.springconfigguard.project;

import com.intellij.openapi.application.ReadAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;

import java.util.List;

public final class ProjectConfigCollectorTest extends BasePlatformTestCase {

    public void testCollectsOnlySpringApplicationConfigurationFromProjectContent() {
        myFixture.addFileToProject(
                "src/main/resources/application.yml",
                "payment:\n  api:\n    url: http://localhost:8080\n"
        );
        myFixture.addFileToProject(
                "src/main/resources/application-prod.properties",
                "payment.timeout=1000\n"
        );
        myFixture.addFileToProject("README.md", "payment.timeout=not-config\n");

        List<ConfigEntry> entries = ReadAction.compute(
                () -> new ProjectConfigCollector().collect(getProject())
        );

        assertTrue(entries.stream().anyMatch(entry ->
                entry.profile().equals("default")
                        && entry.key().equals("payment.api.url")
                        && entry.value().equals("http://localhost:8080")
        ));
        assertTrue(entries.stream().anyMatch(entry ->
                entry.profile().equals("prod")
                        && entry.key().equals("payment.timeout")
                        && entry.value().equals("1000")
        ));
        assertFalse(entries.stream().anyMatch(entry -> entry.value().equals("not-config")));
    }
}
