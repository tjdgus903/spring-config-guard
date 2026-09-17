package io.github.tjdgus903.springconfigguard.rule;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;

import java.util.List;
import java.util.Set;

public final class RuleEngine {
    private final List<ConfigRule> rules;

    public RuleEngine(List<ConfigRule> rules) { this(rules, Set.of()); }

    public RuleEngine(List<ConfigRule> rules, Set<String> disabledRuleIds) {
        Set<String> disabled = Set.copyOf(disabledRuleIds);
        this.rules = rules.stream().filter(rule -> !disabled.contains(rule.id())).toList();
    }

    public List<Finding> analyze(ConfigEntry entry, ConfigContext context) {
        return rules.stream().map(rule -> rule.check(entry, context))
                .flatMap(java.util.Optional::stream).toList();
    }
}
