package io.github.tjdgus903.springconfigguard.rule.rules;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Severity;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionRiskRulesTest {
    private static final ConfigContext PROD = new ConfigContext(true);
    private static final ConfigContext NON_PROD = new ConfigContext(false);

    @Test
    void actuatorWildcardIsHighInProduction() {
        var rule = new ActuatorWildcardExposureRule();
        var finding = rule.check(entry("management.endpoints.web.exposure.include", "health,*"), PROD).orElseThrow();
        assertEquals(Severity.HIGH, finding.severity());
        assertTrue(rule.check(entry("management.endpoints.web.exposure.include", "health,info"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoints.web.exposure.include", "*"), NON_PROD).isEmpty());
    }

    @Test
    void alwaysStacktraceIsHighInProduction() {
        var rule = new StacktraceExposureRule();
        var finding = rule.check(entry("server.error.include-stacktrace", "ALWAYS"), PROD).orElseThrow();
        assertEquals(Severity.HIGH, finding.severity());
        assertTrue(rule.check(entry("server.error.include-stacktrace", "never"), PROD).isEmpty());
        assertTrue(rule.check(entry("server.error.include-stacktrace", "always"), NON_PROD).isEmpty());
    }

    @Test
    void alwaysErrorMessageIsHighInProduction() {
        var rule = new ErrorMessageExposureRule();
        var finding = rule.check(entry("server.error.include-message", " ALWAYS "), PROD).orElseThrow();
        assertEquals(Severity.HIGH, finding.severity());
        assertTrue(rule.check(entry("server.error.include-message", "never"), PROD).isEmpty());
        assertTrue(rule.check(entry("server.error.include-message", "on_param"), PROD).isEmpty());
        assertTrue(rule.check(entry("server.error.include-message", null), PROD).isEmpty());
        assertTrue(rule.check(entry("server.error.include-stacktrace", "always"), PROD).isEmpty());
        assertTrue(rule.check(entry("server.error.include-message", "always"), NON_PROD).isEmpty());
    }

    @Test
    void alwaysBindingErrorsIsHighInProduction() {
        var rule = new BindingErrorsExposureRule();
        var finding = rule.check(entry("server.error.include-binding-errors", " ALWAYS "), PROD).orElseThrow();
        assertEquals(Severity.HIGH, finding.severity());
        assertTrue(rule.check(entry("server.error.include-binding-errors", "never"), PROD).isEmpty());
        assertTrue(rule.check(entry("server.error.include-binding-errors", "on_param"), PROD).isEmpty());
        assertTrue(rule.check(entry("server.error.include-binding-errors", null), PROD).isEmpty());
        assertTrue(rule.check(entry("server.error.include-message", "always"), PROD).isEmpty());
        assertTrue(rule.check(entry("server.error.include-binding-errors", "always"), NON_PROD).isEmpty());
    }

    @Test
    void enabledH2ConsoleIsHighInProduction() {
        var rule = new H2ConsoleExposureRule();
        var finding = rule.check(entry("spring.h2.console.enabled", " TRUE "), PROD).orElseThrow();
        assertEquals(Severity.HIGH, finding.severity());
        assertTrue(rule.check(entry("spring.h2.console.enabled", "false"), PROD).isEmpty());
        assertTrue(rule.check(entry("spring.h2.console.enabled", null), PROD).isEmpty());
        assertTrue(rule.check(entry("spring.h2.console.path", "true"), PROD).isEmpty());
        assertTrue(rule.check(entry("spring.h2.console.enabled", "true"), NON_PROD).isEmpty());
    }

    @Test
    void alwaysVisibleActuatorEnvValuesAreHighInProduction() {
        var rule = new EnvValuesExposureRule();
        var finding = rule.check(entry("management.endpoint.env.show-values", " ALWAYS "), PROD).orElseThrow();
        assertEquals(Severity.HIGH, finding.severity());
        assertTrue(rule.check(entry("management.endpoint.env.show-values", "never"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.env.show-values", "when-authorized"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.env.show-values", null), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.configprops.show-values", "always"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.env.show-values", "always"), NON_PROD).isEmpty());
    }

    @Test
    void alwaysVisibleActuatorConfigPropsValuesAreHighInProduction() {
        var rule = new ConfigPropsValuesExposureRule();
        var finding = rule.check(entry("management.endpoint.configprops.show-values", " ALWAYS "), PROD).orElseThrow();
        assertEquals(Severity.HIGH, finding.severity());
        assertTrue(rule.check(entry("management.endpoint.configprops.show-values", "never"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.configprops.show-values", "when-authorized"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.configprops.show-values", null), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.env.show-values", "always"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.configprops.show-values", "always"), NON_PROD).isEmpty());
    }

    @Test
    void alwaysVisibleActuatorHealthDetailsAreHighInProduction() {
        var rule = new HealthDetailsExposureRule();
        var finding = rule.check(entry("management.endpoint.health.show-details", " ALWAYS "), PROD).orElseThrow();
        assertEquals(Severity.HIGH, finding.severity());
        assertTrue(rule.check(entry("management.endpoint.health.show-details", "never"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.health.show-details", "when-authorized"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.health.show-details", null), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.health.show-components", "always"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.health.show-details", "always"), NON_PROD).isEmpty());
    }

    @Test
    void alwaysVisibleActuatorHealthComponentsAreHighInProduction() {
        var rule = new HealthComponentsExposureRule();
        var finding = rule.check(entry("management.endpoint.health.show-components", " ALWAYS "), PROD).orElseThrow();
        assertEquals(Severity.HIGH, finding.severity());
        assertTrue(rule.check(entry("management.endpoint.health.show-components", "never"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.health.show-components", "when-authorized"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.health.show-components", null), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.health.show-details", "always"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.health.show-components", "always"), NON_PROD).isEmpty());
    }

    @Test
    void enabledActuatorShutdownIsHighInProduction() {
        var rule = new ShutdownEndpointEnabledRule();
        var finding = rule.check(entry("management.endpoint.shutdown.enabled", " TRUE "), PROD).orElseThrow();
        assertEquals(Severity.HIGH, finding.severity());
        assertTrue(rule.check(entry("management.endpoint.shutdown.enabled", "false"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.shutdown.enabled", null), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.health.show-details", "true"), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.shutdown.enabled", "true"), NON_PROD).isEmpty());
    }

    @Test
    void immediateServerShutdownIsWarningInProduction() {
        var rule = new ImmediateServerShutdownRule();
        var finding = rule.check(entry("server.shutdown", " IMMEDIATE "), PROD).orElseThrow();
        assertEquals(Severity.WARNING, finding.severity());
        assertTrue(rule.check(entry("server.shutdown", "graceful"), PROD).isEmpty());
        assertTrue(rule.check(entry("server.shutdown", null), PROD).isEmpty());
        assertTrue(rule.check(entry("management.endpoint.shutdown.enabled", "immediate"), PROD).isEmpty());
        assertTrue(rule.check(entry("server.shutdown", "immediate"), NON_PROD).isEmpty());
    }

    @Test
    void verboseRootLoggingIsWarningInProduction() {
        var rule = new RootDebugLoggingRule();
        var debugFinding = rule.check(entry("logging.level.root", "DEBUG"), PROD).orElseThrow();
        var traceFinding = rule.check(entry("logging.level.root", " TRACE "), PROD).orElseThrow();
        assertEquals(Severity.WARNING, debugFinding.severity());
        assertEquals(Severity.WARNING, traceFinding.severity());
        assertTrue(rule.check(entry("logging.level.root", "INFO"), PROD).isEmpty());
        assertTrue(rule.check(entry("logging.level.root", "WARN"), PROD).isEmpty());
        assertTrue(rule.check(entry("logging.level.root", "ERROR"), PROD).isEmpty());
        assertTrue(rule.check(entry("logging.level.root", "DEBUG"), NON_PROD).isEmpty());
        assertTrue(rule.check(entry("logging.level.root", "TRACE"), NON_PROD).isEmpty());
    }

    @Test
    void showSqlIsWarningInProduction() {
        var rule = new JpaShowSqlRule();
        var finding = rule.check(entry("spring.jpa.show-sql", "true"), PROD).orElseThrow();
        assertEquals(Severity.WARNING, finding.severity());
        assertTrue(rule.check(entry("spring.jpa.show-sql", "false"), PROD).isEmpty());
        assertTrue(rule.check(entry("spring.jpa.show-sql", "true"), NON_PROD).isEmpty());
    }

    private ConfigEntry entry(String key, String value) {
        return new ConfigEntry(key, value, "prod", "application-prod.yml", 1);
    }
}
