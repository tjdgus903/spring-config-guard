package io.github.tjdgus903.springconfigguard.code;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

public final class SpringValuePsiExtractorTest extends BasePlatformTestCase {
    private final SpringValuePsiExtractor extractor = new SpringValuePsiExtractor();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject(
                "src/org/springframework/beans/factory/annotation/Value.java",
                "package org.springframework.beans.factory.annotation; public @interface Value { String value(); }"
        );
        myFixture.addFileToProject(
                "src/com/example/Value.java",
                "package com.example; public @interface Value { String value(); }"
        );
    }

    public void testExtractsImportedAndFullyQualifiedSpringValueUsages() {
        myFixture.configureByText("Example.java", """
                import org.springframework.beans.factory.annotation.Value;

                class Example {
                    @Value("${payment.api.url:http://localhost:8080}")
                    String paymentUrl;

                    @org.springframework.beans.factory.annotation.Value("${feature.enabled:false}")
                    boolean featureEnabled;
                }
                """);

        List<ConfigUsage> usages = extractor.extract(myFixture.getFile());
        assertEquals(2, usages.size());
        assertEquals("payment.api.url", usages.get(0).key());
        assertEquals("http://localhost:8080", usages.get(0).defaultValue());
        assertEquals(4, usages.get(0).line());
        assertEquals("feature.enabled", usages.get(1).key());
        assertEquals("false", usages.get(1).defaultValue());
    }

    public void testIgnoresCustomValueAnnotation() {
        myFixture.configureByText("CustomExample.java", """
                import com.example.Value;

                class CustomExample {
                    @Value("${should.not.match}")
                    String value;
                }
                """);

        assertEmpty(extractor.extract(myFixture.getFile()));
    }

    public void testIgnoresNonLiteralAndSpelOnlySpringValues() {
        myFixture.configureByText("IgnoredExample.java", """
                import org.springframework.beans.factory.annotation.Value;

                class IgnoredExample {
                    static final String KEY = "${constant.reference}";

                    @Value(KEY)
                    String nonLiteral;

                    @Value("#{systemProperties['user.home']}")
                    String spelOnly;
                }
                """);

        assertEmpty(extractor.extract(myFixture.getFile()));
    }
}
