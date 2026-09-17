package io.github.tjdgus903.springconfigguard.rule;

import io.github.tjdgus903.springconfigguard.rule.rules.ActuatorWildcardExposureRule;
import io.github.tjdgus903.springconfigguard.rule.rules.BindingErrorsExposureRule;
import io.github.tjdgus903.springconfigguard.rule.rules.ConfigPropsValuesExposureRule;
import io.github.tjdgus903.springconfigguard.rule.rules.DdlAutoRule;
import io.github.tjdgus903.springconfigguard.rule.rules.ErrorMessageExposureRule;
import io.github.tjdgus903.springconfigguard.rule.rules.EnvValuesExposureRule;
import io.github.tjdgus903.springconfigguard.rule.rules.H2ConsoleExposureRule;
import io.github.tjdgus903.springconfigguard.rule.rules.HealthComponentsExposureRule;
import io.github.tjdgus903.springconfigguard.rule.rules.HealthDetailsExposureRule;
import io.github.tjdgus903.springconfigguard.rule.rules.JpaShowSqlRule;
import io.github.tjdgus903.springconfigguard.rule.rules.RootDebugLoggingRule;
import io.github.tjdgus903.springconfigguard.rule.rules.StacktraceExposureRule;

import java.util.List;
import java.util.Set;

public final class MvpRuleRegistry {
    private MvpRuleRegistry() {}

    public static List<ConfigRule> rules() {
        return List.of(new DdlAutoRule(), new ActuatorWildcardExposureRule(), new StacktraceExposureRule(),
                new RootDebugLoggingRule(), new JpaShowSqlRule(), new ErrorMessageExposureRule(),
                new BindingErrorsExposureRule(), new H2ConsoleExposureRule(), new EnvValuesExposureRule(),
                new ConfigPropsValuesExposureRule(), new HealthDetailsExposureRule(), new HealthComponentsExposureRule());
    }

    public static RuleEngine ruleEngine() { return new RuleEngine(rules()); }
    public static RuleEngine ruleEngine(Set<String> disabledRuleIds) {
        return new RuleEngine(rules(), disabledRuleIds);
    }
}
