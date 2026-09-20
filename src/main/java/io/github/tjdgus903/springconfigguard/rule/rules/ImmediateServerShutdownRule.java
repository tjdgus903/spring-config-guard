package io.github.tjdgus903.springconfigguard.rule.rules;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.model.Severity;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import io.github.tjdgus903.springconfigguard.rule.ConfigRule;

import java.util.Locale;
import java.util.Optional;

/** Detects immediate server shutdown being explicitly selected in production. */
public final class ImmediateServerShutdownRule implements ConfigRule {
    public static final String RULE_ID = "SCG014";
    private static final String KEY = "server.shutdown";

    @Override public String id() { return RULE_ID; }

    @Override
    public Optional<Finding> check(ConfigEntry entry, ConfigContext context) {
        if (!context.productionProfile() || !KEY.equals(entry.key()) || entry.value() == null) return Optional.empty();
        if (!"immediate".equals(entry.value().trim().toLowerCase(Locale.ROOT))) return Optional.empty();
        return Optional.of(new Finding(
                RULE_ID,
                Severity.WARNING,
                "Immediate server shutdown configured in production",
                "server.shutdown=immediate skips graceful shutdown and can terminate in-flight requests during application shutdown.",
                entry
        ));
    }
}
