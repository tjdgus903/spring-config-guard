package io.github.tjdgus903.springconfigguard.rule;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.rule.rules.DdlAutoRule;
import io.github.tjdgus903.springconfigguard.rule.rules.RootDebugLoggingRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEngineDisabledRulesTest {
    private static final ConfigContext PRODUCTION = new ConfigContext(true);

    @Test
    void disabledRuleDoesNotEmitFinding() {
        RuleEngine engine = new RuleEngine(
                List.of(new RootDebugLoggingRule()),
                Set.of(RootDebugLoggingRule.RULE_ID));

        assertTrue(engine.analyze(entry("logging.level.root", "DEBUG"), PRODUCTION).isEmpty());
    }

    @Test
    void enabledRuleEmitsFindingAgain() {
        RuleEngine engine = new RuleEngine(List.of(new RootDebugLoggingRule()), Set.of());

        assertEquals(List.of(RootDebugLoggingRule.RULE_ID),
                engine.analyze(entry("logging.level.root", "DEBUG"), PRODUCTION).stream()
                        .map(finding -> finding.ruleId())
                        .toList());
    }

    @Test
    void disablingOneRuleDoesNotSuppressAnotherRule() {
        RuleEngine engine = new RuleEngine(
                List.of(new RootDebugLoggingRule(), new DdlAutoRule()),
                Set.of(RootDebugLoggingRule.RULE_ID));

        assertEquals(List.of(DdlAutoRule.RULE_ID),
                engine.analyze(entry("spring.jpa.hibernate.ddl-auto", "update"), PRODUCTION).stream()
                        .map(finding -> finding.ruleId())
                        .toList());
    }

    private static ConfigEntry entry(String key, String value) {
        return new ConfigEntry(key, value, "prod", "application-prod.yml", 1);
    }
}
