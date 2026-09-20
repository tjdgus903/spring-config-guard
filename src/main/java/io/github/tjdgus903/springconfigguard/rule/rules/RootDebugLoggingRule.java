package io.github.tjdgus903.springconfigguard.rule.rules;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.model.Severity;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import io.github.tjdgus903.springconfigguard.rule.ConfigRule;

import java.util.Locale;
import java.util.Optional;

/** Detects verbose root DEBUG or TRACE logging in production. */
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

        String level = entry.value().trim().toLowerCase(Locale.ROOT);
        if (!"debug".equals(level) && !"trace".equals(level)) {
            return Optional.empty();
        }

        return Optional.of(new Finding(
                RULE_ID,
                Severity.WARNING,
                "Verbose root logging enabled in production",
                "logging.level.root=DEBUG or TRACE can increase log volume and may expose sensitive runtime details.",
                entry
        ));
    }
}
