package io.github.tjdgus903.springconfigguard.inspection;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public final class SpringConfigGuardInspectionTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "";
    }

    public void testHighlightsAllMvpRulesInProductionProperties() {
        myFixture.enableInspections(new SpringConfigGuardInspection());
        myFixture.configureByText(
                "application-prod.properties",
                "spring.jpa.hibernate.ddl-auto=<warning descr=\"[SCG001][CRITICAL] Risky Hibernate schema management setting in production\">create</warning>\n" +
                        "management.endpoints.web.exposure.include=<warning descr=\"[SCG002][HIGH] Wildcard Actuator endpoint exposure in production\">*</warning>\n" +
                        "server.error.include-stacktrace=<warning descr=\"[SCG003][HIGH] Stacktraces are always exposed in production\">always</warning>\n" +
                        "logging.level.root=<warning descr=\"[SCG004][WARNING] Root DEBUG logging enabled in production\">DEBUG</warning>\n" +
                        "spring.jpa.show-sql=<warning descr=\"[SCG005][WARNING] Hibernate show-sql enabled in production\">true</warning>\n"
        );

        myFixture.checkHighlighting(true, false, false);
    }

    public void testHighlightsYamlFindingInProductionProfile() {
        myFixture.enableInspections(new SpringConfigGuardInspection());
        myFixture.configureByText(
                "application-production.yml",
                "management:\n" +
                        "  endpoints:\n" +
                        "    web:\n" +
                        "      exposure:\n" +
                        "        include: \"<warning descr=\"[SCG002][HIGH] Wildcard Actuator endpoint exposure in production\">*</warning>\"\n"
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

    public void testDoesNotHighlightMvpRisksInDevelopmentProfile() {
        myFixture.enableInspections(new SpringConfigGuardInspection());
        myFixture.configureByText(
                "application-dev.properties",
                "spring.jpa.hibernate.ddl-auto=create\n" +
                        "management.endpoints.web.exposure.include=*\n" +
                        "server.error.include-stacktrace=always\n" +
                        "logging.level.root=DEBUG\n" +
                        "spring.jpa.show-sql=true\n"
        );

        myFixture.checkHighlighting(true, false, false);
    }
}
