package io.github.tjdgus903.springconfigguard.rule;

import io.github.tjdgus903.springconfigguard.rule.rules.ActuatorWildcardExposureRule;
import io.github.tjdgus903.springconfigguard.rule.rules.DdlAutoRule;
import io.github.tjdgus903.springconfigguard.rule.rules.JpaShowSqlRule;
import io.github.tjdgus903.springconfigguard.rule.rules.RootDebugLoggingRule;
import io.github.tjdgus903.springconfigguard.rule.rules.StacktraceExposureRule;

import java.util.List;

/** Central registry for the deterministic MVP rule set. */
public final class MvpRuleRegistry {
    private MvpRuleRegistry() {
    }

    public static List<ConfigRule> rules() {
        return List.of(
                new DdlAutoRule(),
                new ActuatorWildcardExposureRule(),
                new StacktraceExposureRule(),
                new RootDebugLoggingRule(),
                new JpaShowSqlRule()
        );
    }

    public static RuleEngine ruleEngine() {
        return new RuleEngine(rules());
    }
}
