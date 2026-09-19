package io.github.tjdgus903.springconfigguard.inspection;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public final class SpringConfigGuardInspectionTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() { return ""; }

    public void testHighlightsAllMvpRulesInProductionProperties() {
        myFixture.enableInspections(new SpringConfigGuardInspection());
        myFixture.configureByText("application-prod.properties",
                warning("spring.jpa.hibernate.ddl-auto", "SCG001", "CRITICAL", "Risky Hibernate schema management setting in production", "Use validate or none in production; apply schema changes through reviewed migrations.", "create") +
                warning("management.endpoints.web.exposure.include", "SCG002", "HIGH", "Wildcard Actuator endpoint exposure in production", "Expose only required Actuator endpoints and protect them with authentication.", "*") +
                warning("server.error.include-stacktrace", "SCG003", "HIGH", "Stacktraces are always exposed in production", "Disable stack traces in production responses and log details securely on the server.", "always") +
                warning("logging.level.root", "SCG004", "WARNING", "Root DEBUG logging enabled in production", "Keep root logging at INFO or higher in production unless a controlled diagnostic window is active.", "DEBUG") +
                warning("spring.jpa.show-sql", "SCG005", "WARNING", "Hibernate show-sql enabled in production", "Disable Hibernate SQL logging in production or route it to a controlled, access-restricted logger.", "true") +
                warning("server.error.include-message", "SCG006", "HIGH", "Error messages are always exposed in production", "Return generic production error messages and keep internal details in protected server logs.", "always") +
                warning("server.error.include-binding-errors", "SCG007", "HIGH", "Binding errors are always exposed in production", "Avoid exposing binding errors to clients; return a generic validation response.", "always") +
                warning("spring.h2.console.enabled", "SCG008", "HIGH", "H2 console enabled in production", "Disable the H2 console outside local development.", "true") +
                warning("management.endpoint.env.show-values", "SCG009", "HIGH", "Actuator environment values are always shown in production", "Use never or when-authorized for Actuator environment value sanitization in production.", "always") +
                warning("management.endpoint.configprops.show-values", "SCG010", "HIGH", "Actuator configuration values are always shown in production", "Use never or when-authorized for Actuator configprops value sanitization in production.", "always") +
                warning("management.endpoint.health.show-details", "SCG011", "HIGH", "Actuator health details are always shown in production", "Use never or when-authorized for Actuator health details in production.", "always") +
                warning("management.endpoint.health.show-components", "SCG012", "HIGH", "Actuator health components are always shown in production", "Use never or when-authorized for Actuator health components in production.", "always"));
        myFixture.checkHighlighting(true, false, false);
    }

    public void testHighlightsYamlFindingInProductionProfile() {
        myFixture.enableInspections(new SpringConfigGuardInspection());
        myFixture.configureByText("application-production.yml",
                "management:\n  endpoints:\n    web:\n      exposure:\n        include: " +
                warningValue("SCG002", "HIGH", "Wildcard Actuator endpoint exposure in production", "Expose only required Actuator endpoints and protect them with authentication.", "\"*\"") );
        myFixture.checkHighlighting(true, false, false);
    }

    public void testHighlightsWarningDdlAutoUpdateInProductionProperties() {
        myFixture.enableInspections(new SpringConfigGuardInspection());
        myFixture.configureByText("application-production.properties",
                warning("spring.jpa.hibernate.ddl-auto", "SCG001", "WARNING", "Risky Hibernate schema management setting in production", "Use validate or none in production; apply schema changes through reviewed migrations.", "update"));
        myFixture.checkHighlighting(true, false, false);
    }

    public void testDoesNotHighlightMvpRisksInDevelopmentProfile() {
        myFixture.enableInspections(new SpringConfigGuardInspection());
        myFixture.configureByText("application-dev.properties",
                "spring.jpa.hibernate.ddl-auto=create\nmanagement.endpoints.web.exposure.include=*\nserver.error.include-stacktrace=always\nlogging.level.root=DEBUG\nspring.jpa.show-sql=true\nserver.error.include-message=always\nserver.error.include-binding-errors=always\nspring.h2.console.enabled=true\nmanagement.endpoint.env.show-values=always\nmanagement.endpoint.configprops.show-values=always\nmanagement.endpoint.health.show-details=always\nmanagement.endpoint.health.show-components=always\n");
        myFixture.checkHighlighting(true, false, false);
    }

    private static String warning(String key, String rule, String severity, String message, String guidance, String value) {
        return key + "=" + warningValue(rule, severity, message, guidance, value) + "\n";
    }

    private static String warningValue(String rule, String severity, String message, String guidance, String value) {
        return "<warning descr=\"[" + rule + "][" + severity + "] " + message + " Safe remediation: " + guidance + "\">" + value + "</warning>";
    }
}
