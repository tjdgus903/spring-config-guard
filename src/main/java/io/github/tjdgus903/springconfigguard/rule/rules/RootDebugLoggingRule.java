package io.github.tjdgus903.springconfigguard.rule.rules;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.model.Severity;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import io.github.tjdgus903.springconfigguard.rule.ConfigRule;

import java.util.Locale;
import java.util.Optional;

/** Detects root DEBUG logging in production. */
public final class RootDebugLoggingRule implements ConfigRule {
    public static final String RULE_ID = "SCG004";
    private static final String KEY = "logging.level.root";

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public Optional<Finding> check(ConfigEntry entry, ConfigContext context) {
        if (!context.productionProfile() || !KEY.equals(entry.key()) || entry.value() == null) {
            return Optional.empty();
        }

        if (!"debug".equals(entry.value().trim().toLowerCase(Locale.ROOT))) {
            return Optional.empty();
        }

        return Optional.of(new Finding(
                RULE_ID,
                Severity.WARNING,
                "Root DEBUG logging enabled in production",
                "logging.level.root=DEBUG can increase log volume and may expose sensitive runtime details.",
                entry
        ));
    }
}
