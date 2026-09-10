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
    void rootDebugIsWarningInProduction() {
        var rule = new RootDebugLoggingRule();
        var finding = rule.check(entry("logging.level.root", "DEBUG"), PROD).orElseThrow();
        assertEquals(Severity.WARNING, finding.severity());
        assertTrue(rule.check(entry("logging.level.root", "INFO"), PROD).isEmpty());
        assertTrue(rule.check(entry("logging.level.root", "DEBUG"), NON_PROD).isEmpty());
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
