package io.github.tjdgus903.springconfigguard.inspection;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;

public final class SpringConfigGuardInspectionTest extends LightPlatformCodeInsightFixtureTestCase {

    public void testHighlightsCriticalDdlAutoInProductionProperties() {
        myFixture.enableInspections(new SpringConfigGuardInspection());
        myFixture.configureByText(
                "application-prod.properties",
                "spring.jpa.hibernate.ddl-auto=<warning descr=\"[SCG001][CRITICAL] Risky Hibernate schema management setting in production\">create</warning>\n"
        );

        myFixture.checkHighlighting(true, false, false);
    }

    public void testHighlightsWarningDdlAutoUpdateInProductionProperties() {
        myFixture.enableInspections(new SpringConfigGuardInspection());
        myFixture.configureByText(
                "application-production.properties",
                "spring.jpa.hibernate.ddl-auto=<warning descr=\"[SCG001][WARNING] Risky Hibernate schema management setting in production\">update</warning>\n"
        );

        myFixture.checkHighlighting(true, false, false);
    }

    public void testDoesNotHighlightDdlAutoInDevelopmentProfile() {
        myFixture.enableInspections(new SpringConfigGuardInspection());
        myFixture.configureByText(
                "application-dev.properties",
                "spring.jpa.hibernate.ddl-auto=create\n"
        );

        myFixture.checkHighlighting(true, false, false);
    }
}
