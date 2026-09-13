package io.github.tjdgus903.springconfigguard.rule.rules;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.model.Severity;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import io.github.tjdgus903.springconfigguard.rule.ConfigRule;

import java.util.Locale;
import java.util.Optional;

/** Detects the development-only H2 web console being enabled in production. */
public final class H2ConsoleExposureRule implements ConfigRule {
    public static final String RULE_ID = "SCG008";
    private static final String KEY = "spring.h2.console.enabled";

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public Optional<Finding> check(ConfigEntry entry, ConfigContext context) {
        if (!context.productionProfile() || !KEY.equals(entry.key()) || entry.value() == null) {
            return Optional.empty();
        }

        if (!"true".equals(entry.value().trim().toLowerCase(Locale.ROOT))) {
            return Optional.empty();
        }

        return Optional.of(new Finding(
                RULE_ID,
                Severity.HIGH,
                "H2 console enabled in production",
                "spring.h2.console.enabled=true exposes a development-only database console in production.",
                entry
        ));
    }
}
