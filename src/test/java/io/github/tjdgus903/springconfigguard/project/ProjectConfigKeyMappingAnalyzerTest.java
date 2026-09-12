package io.github.tjdgus903.springconfigguard.project;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import io.github.tjdgus903.springconfigguard.code.ConfigUsage;
import io.github.tjdgus903.springconfigguard.code.ConfigurationPropertyMapping;
import io.github.tjdgus903.springconfigguard.mapping.ConfigKeyMatch;
import io.github.tjdgus903.springconfigguard.mapping.ConfigKeyMappingAnalysis;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProjectConfigKeyMappingAnalyzerTest extends LightJavaCodeInsightFixtureTestCase {
    private final ProjectConfigKeyMappingAnalyzer analyzer = new ProjectConfigKeyMappingAnalyzer();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addClass("package org.springframework.beans.factory.annotation; public @interface Value { String value(); }");
        myFixture.addClass("""
                package org.springframework.boot.context.properties;
                public @interface ConfigurationProperties {
                    String value() default "";
                    String prefix() default "";
                }
                """);
    }

    public void testConnectsProjectFilesToBothJavaExtractorsAndPreservesProfiles() {
        myFixture.addFileToProject("resources/application.yml", """
                service:
                  url: https://default.example
                payment:
                  database:
                    max-size: 12
                config:
                  only: true
                """);
        myFixture.addFileToProject("resources/application-prod.properties", "service.url=https://prod.example\n");
        myFixture.addFileToProject("com/acme/Client.java", """
                package com.acme;
                import org.springframework.beans.factory.annotation.Value;
                class Client {
                    @Value("${service.url}") String url;
                    @Value("${external.token:local-default}") String token;
                }
                """);
        myFixture.addFileToProject("com/acme/PaymentProperties.java", """
                package com.acme;
                import org.springframework.boot.context.properties.ConfigurationProperties;
                @ConfigurationProperties(prefix = "payment")
                class PaymentProperties {
                    Database database;
                    String region;
                    static class Database { int maxSize; }
                }
                """);

        ConfigKeyMappingAnalysis analysis = analyze();

        assertEquals(List.of("service.url", "payment.database.max-size"),
                analysis.matches().stream().map(ConfigKeyMatch::key).toList());
        ConfigKeyMatch url = analysis.matches().get(0);
        assertEquals(Set.of("default", "prod"), url.configEntries().stream()
                .map(ConfigEntry::profile).collect(Collectors.toSet()));
        assertEquals(2, url.configEntries().size());
        assertEquals(4, url.valueUsages().get(0).line());
        assertTrue(url.valueUsages().get(0).filePath().endsWith("com/acme/Client.java"));
        assertEquals("maxSize", analysis.matches().get(1).propertyMappings().get(0).fieldName());
        assertEquals(7, analysis.matches().get(1).propertyMappings().get(0).line());
        assertEquals(List.of("config.only"), analysis.unmatchedConfigEntries().stream().map(ConfigEntry::key).toList());
        assertEquals(List.of("external.token"), analysis.unmatchedValueUsages().stream().map(ConfigUsage::key).toList());
        assertEquals("local-default", analysis.unmatchedValueUsages().get(0).defaultValue());
        assertTrue(analysis.unmatchedValueUsagesWithoutDefault().isEmpty());
        assertEquals(List.of("external.token"), analysis.unmatchedValueUsagesWithDefault().stream()
                .map(ConfigUsage::key).toList());
        assertEquals(List.of("payment.region"), analysis.unmatchedPropertyMappings().stream()
                .map(ConfigurationPropertyMapping::key).toList());
    }

    public void testUsesUnsavedConfigAndJavaEditsOnEveryAnalysis() {
        PsiFile config = myFixture.addFileToProject("application.properties", "old.key=before\n");
        PsiFile java = myFixture.addFileToProject("Client.java", """
                import org.springframework.beans.factory.annotation.Value;
                class Client { @Value("${old.key}") String value; }
                """);
        assertEquals("old.key", analyze().matches().get(0).key());

        Document configDocument = FileDocumentManager.getInstance().getDocument(config.getVirtualFile());
        Document javaDocument = PsiDocumentManager.getInstance(getProject()).getDocument(java);
        assertNotNull(configDocument);
        assertNotNull(javaDocument);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            configDocument.setText("new.key=after\n");
            javaDocument.setText(javaDocument.getText().replace("old.key", "new.key"));
        });

        ConfigKeyMappingAnalysis analysis = analyze();
        assertTrue(FileDocumentManager.getInstance().isDocumentUnsaved(configDocument));
        assertEquals(List.of("new.key"), analysis.matches().stream().map(ConfigKeyMatch::key).toList());
        assertEquals("after", analysis.matches().get(0).configEntries().get(0).value());
        assertEmpty(analysis.unmatchedConfigEntries());
        assertEmpty(analysis.unmatchedValueUsages());
    }

    public void testIgnoresExcludedContentNonJavaFilesAndUnrelatedConfiguration() {
        myFixture.addFileToProject("application.properties", "visible.key=true\n");
        PsiFile excluded = myFixture.addFileToProject("excluded/Hidden.java", """
                import org.springframework.beans.factory.annotation.Value;
                class Hidden { @Value("${visible.key}") String value; }
                """);
        myFixture.addFileToProject("excluded/application-prod.properties", "excluded.key=true\n");
        PsiTestUtil.addExcludedRoot(myFixture.getModule(), excluded.getVirtualFile().getParent());
        myFixture.addFileToProject("Example.txt", "@Value(\"${visible.key}\")");
        myFixture.addFileToProject("other.properties", "unrelated.key=true\n");

        ConfigKeyMappingAnalysis analysis = analyze();
        assertEmpty(analysis.matches());
        assertEmpty(analysis.unmatchedValueUsages());
        assertEmpty(analysis.unmatchedPropertyMappings());
        assertEquals(List.of("visible.key"), analysis.unmatchedConfigEntries().stream().map(ConfigEntry::key).toList());
    }

    public void testKeepsDuplicateJavaReferencesInDeterministicPathOrder() {
        myFixture.addFileToProject("application.properties", "shared.key=true\n");
        for (String name : List.of("ZClient", "AClient")) {
            myFixture.addFileToProject(name + ".java", """
                    import org.springframework.beans.factory.annotation.Value;
                    class %s { @Value("${shared.key}") String value; }
                    """.formatted(name));
        }

        List<ConfigUsage> usages = analyze().matches().get(0).valueUsages();
        assertEquals(2, usages.size());
        assertTrue(usages.get(0).filePath().endsWith("AClient.java"));
        assertTrue(usages.get(1).filePath().endsWith("ZClient.java"));
    }

    private ConfigKeyMappingAnalysis analyze() {
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        return ReadAction.compute(() -> analyzer.analyze(getProject()));
    }
}
