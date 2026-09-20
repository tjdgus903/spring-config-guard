package io.github.tjdgus903.springconfigguard.rule;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.rule.rules.RootDebugLoggingRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MvpRuleRegistryTest {

    @Test
    void exposesAllMvpRulesInStableOrderWithoutDuplicates() {
        List<String> ids = MvpRuleRegistry.rules().stream()
                .map(ConfigRule::id)
                .toList();

        assertEquals(List.of("SCG001", "SCG002", "SCG003", "SCG004", "SCG005", "SCG006", "SCG007", "SCG008", "SCG009", "SCG010", "SCG011", "SCG012", "SCG013"), ids);
        Set<String> unique = ids.stream().collect(Collectors.toSet());
        assertEquals(ids.size(), unique.size());
    }

    @Test
    void registryEngineHonorsDisabledRuleIdsWithoutSuppressingOthers() {
        RuleEngine engine = MvpRuleRegistry.ruleEngine(Set.of(RootDebugLoggingRule.RULE_ID));
        ConfigContext production = new ConfigContext(true);

        assertTrue(engine.analyze(entry("logging.level.root", "DEBUG"), production).isEmpty());
        assertEquals(List.of("SCG001"),
                engine.analyze(entry("spring.jpa.hibernate.ddl-auto", "update"), production).stream()
                        .map(finding -> finding.ruleId())
                        .toList());
    }

    private static ConfigEntry entry(String key, String value) {
        return new ConfigEntry(key, value, "prod", "application-prod.yml", 1);
    }
}
