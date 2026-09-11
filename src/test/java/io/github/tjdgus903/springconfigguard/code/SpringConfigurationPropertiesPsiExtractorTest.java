package io.github.tjdgus903.springconfigguard.code;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

import java.util.List;

public final class SpringConfigurationPropertiesPsiExtractorTest extends LightJavaCodeInsightFixtureTestCase {
    private final SpringConfigurationPropertiesPsiExtractor extractor =
            new SpringConfigurationPropertiesPsiExtractor();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addClass("""
                package org.springframework.boot.context.properties;
                public @interface ConfigurationProperties {
                    String value() default "";
                    String prefix() default "";
                }
                """);
        myFixture.addClass("""
                package com.example;
                public @interface ConfigurationProperties {
                    String prefix() default "";
                }
                """);
    }

    public void testExtractsPrefixAndDirectFields() {
        myFixture.configureByText("PaymentProperties.java", """
                package com.acme;

                import org.springframework.boot.context.properties.ConfigurationProperties;

                @ConfigurationProperties(prefix = "payment.api")
                class PaymentProperties {
                    String url;
                    int maxRetryCount;
                    static String GLOBAL;
                }
                """);

        List<ConfigurationPropertyMapping> mappings = extractor.extract(myFixture.getFile());

        assertEquals(2, mappings.size());
        assertEquals("payment.api.url", mappings.get(0).key());
        assertEquals("payment.api", mappings.get(0).prefix());
        assertEquals("com.acme.PaymentProperties", mappings.get(0).declaringClass());
        assertEquals("url", mappings.get(0).fieldName());
        assertTrue(mappings.get(0).filePath().endsWith("PaymentProperties.java"));
        assertTrue(mappings.get(0).line() > 0);

        assertEquals("payment.api.max-retry-count", mappings.get(1).key());
        assertEquals("maxRetryCount", mappings.get(1).fieldName());
    }

    public void testSupportsFullyQualifiedAnnotationAndValueAlias() {
        myFixture.configureByText("FeatureProperties.java", """
                package com.acme;

                @org.springframework.boot.context.properties.ConfigurationProperties("feature")
                class FeatureProperties {
                    boolean enabled;
                }
                """);

        List<ConfigurationPropertyMapping> mappings = extractor.extract(myFixture.getFile());

        assertEquals(1, mappings.size());
        assertEquals("feature.enabled", mappings.get(0).key());
        assertEquals("feature", mappings.get(0).prefix());
        assertEquals("enabled", mappings.get(0).fieldName());
    }

    public void testIgnoresCustomAnnotationWithSameSimpleName() {
        myFixture.configureByText("CustomProperties.java", """
                package com.acme;

                import com.example.ConfigurationProperties;

                @ConfigurationProperties(prefix = "custom")
                class CustomProperties {
                    String value;
                }
                """);

        assertEmpty(extractor.extract(myFixture.getFile()));
    }

    public void testSkipsNonLiteralPrefixInsteadOfGuessing() {
        myFixture.configureByText("DynamicProperties.java", """
                package com.acme;

                import org.springframework.boot.context.properties.ConfigurationProperties;

                final class Constants {
                    static final String PREFIX = "dynamic";
                }

                @ConfigurationProperties(prefix = Constants.PREFIX)
                class DynamicProperties {
                    String value;
                }
                """);

        assertEmpty(extractor.extract(myFixture.getFile()));
    }
}
