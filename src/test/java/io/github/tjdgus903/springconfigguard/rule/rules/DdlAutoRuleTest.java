package io.github.tjdgus903.springconfigguard.rule.rules;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Severity;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DdlAutoRuleTest {
    private final DdlAutoRule rule = new DdlAutoRule();

    @Test
    void createIsCriticalInProduction() {
        var entry = new ConfigEntry("spring.jpa.hibernate.ddl-auto", "create", "prod", "application-prod.yml", 4);
        var finding = rule.check(entry, new ConfigContext(true));

        assertTrue(finding.isPresent());
        assertEquals(Severity.CRITICAL, finding.orElseThrow().severity());
    }

    @Test
    void updateIsWarningInProduction() {
        var entry = new ConfigEntry("spring.jpa.hibernate.ddl-auto", "update", "prod", "application-prod.yml", 4);
        var finding = rule.check(entry, new ConfigContext(true));

        assertTrue(finding.isPresent());
        assertEquals(Severity.WARNING, finding.orElseThrow().severity());
    }

    @Test
    void riskyValueIsIgnoredOutsideProduction() {
        var entry = new ConfigEntry("spring.jpa.hibernate.ddl-auto", "create", "dev", "application-dev.yml", 4);
        assertTrue(rule.check(entry, new ConfigContext(false)).isEmpty());
    }

    @Test
    void safeValueIsIgnored() {
        var entry = new ConfigEntry("spring.jpa.hibernate.ddl-auto", "validate", "prod", "application-prod.yml", 4);
        assertTrue(rule.check(entry, new ConfigContext(true)).isEmpty());
    }
}
