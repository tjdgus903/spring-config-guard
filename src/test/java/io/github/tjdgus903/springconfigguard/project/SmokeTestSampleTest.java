package io.github.tjdgus903.springconfigguard.project;

import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import io.github.tjdgus903.springconfigguard.code.ConfigUsage;
import io.github.tjdgus903.springconfigguard.code.ConfigurationPropertyMapping;
import io.github.tjdgus903.springconfigguard.drift.ProfileDriftAnalyzer;
import io.github.tjdgus903.springconfigguard.drift.ProfileDriftKind;
import io.github.tjdgus903.springconfigguard.inspection.SpringConfigGuardInspection;
import io.github.tjdgus903.springconfigguard.mapping.ConfigKeyMatch;
import io.github.tjdgus903.springconfigguard.mapping.ConfigKeyMappingReportFormatter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Runs the committed manual sample through the plugin so its documented expectations cannot drift. */
public final class SmokeTestSampleTest extends LightJavaCodeInsightFixtureTestCase {
    @Override
    protected String getTestDataPath() {
        return System.getProperty("scg.sample.dir", "samples/config-mapping");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Fixture-only annotation declarations; the standalone sample compiles against real Spring in CI.
        myFixture.addClass("package org.springframework.beans.factory.annotation; public @interface Value { String value(); }");
        myFixture.addClass("""
                package org.springframework.boot.context.properties;
                public @interface ConfigurationProperties {
                    String value() default "";
                    String prefix() default "";
                }
                """);
        myFixture.copyDirectoryToProject("src/main/java", "");
        myFixture.copyDirectoryToProject("src/main/resources", "");
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    }

    public void testSampleMappingCountsAndValueOmissionMatchTheGuide() {
        var analysis = ReadAction.compute(() -> new ProjectConfigKeyMappingAnalyzer().analyze(getProject()));

        assertEquals(Set.of("demo.service.url", "demo.max-retries", "demo.client.timeout-ms"),
                analysis.matches().stream().map(ConfigKeyMatch::key).collect(Collectors.toSet()));
        assertEquals(12, analysis.unmatchedConfigEntries().size()); // 2 default + 5 prod + 5 dev entries.
        assertEquals(Set.of("demo.remote.token", "demo.required.key"),
                analysis.unmatchedValueUsages().stream().map(ConfigUsage::key).collect(Collectors.toSet()));
        assertEquals(List.of("demo.required.key"), analysis.unmatchedValueUsagesWithoutDefault().stream()
                .map(ConfigUsage::key).toList());
        assertEquals(List.of("demo.remote.token"), analysis.unmatchedValueUsagesWithDefault().stream()
                .map(ConfigUsage::key).toList());
        assertEquals(List.of("demo.region"), analysis.unmatchedPropertyMappings().stream()
                .map(ConfigurationPropertyMapping::key).toList());
        String report = new ConfigKeyMappingReportFormatter().format(analysis);
        assertFalse(report.contains("DEMO_DEFAULT_DO_NOT_USE"));
        assertFalse(report.contains("SAMPLE_ONLY_NO_JAVA_REFERENCE"));
        assertFalse(report.contains("http://localhost"));
    }

    public void testSampleInspectionsFlagProductionAndIgnoreDevelopment() {
        myFixture.enableInspections(new SpringConfigGuardInspection());
        myFixture.configureByFile("src/main/resources/application-prod.properties");

        List<String> descriptions = guardDescriptions();
        assertEquals(5, descriptions.size());
        for (String id : List.of("SCG001", "SCG002", "SCG003", "SCG004", "SCG005")) {
            assertTrue(descriptions.stream().anyMatch(description -> description.startsWith("[" + id + "]")));
        }

        myFixture.configureByFile("src/main/resources/application-dev.properties");
        assertEmpty(guardDescriptions());
    }

    public void testSampleDriftDetectsOnlyTheInheritedProductionAuditEndpoint() {
        var analysis = ReadAction.compute(() -> new ProfileDriftAnalyzer().analyze(
                new ProjectConfigCollector().collect(getProject())));
        var risks = analysis.findings().stream().filter(f -> f.kind() == ProfileDriftKind.RISK).toList();

        assertEquals(1, risks.size());
        assertEquals("SCG-PD001", risks.get(0).ruleId());
        assertEquals("prod", risks.get(0).profile());
        assertEquals("demo.audit.url", risks.get(0).key());
    }

    private List<String> guardDescriptions() {
        return myFixture.doHighlighting().stream()
                .map(info -> info.getDescription())
                .filter(description -> description != null && description.startsWith("[SCG"))
                .toList();
    }
}
