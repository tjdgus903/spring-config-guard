package io.github.tjdgus903.springconfigguard.rule;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;

import java.util.List;

public final class RuleEngine {
    private final List<ConfigRule> rules;

    public RuleEngine(List<ConfigRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<Finding> analyze(ConfigEntry entry, ConfigContext context) {
        return rules.stream()
                .map(rule -> rule.check(entry, context))
                .flatMap(java.util.Optional::stream)
                .toList();
    }
}
