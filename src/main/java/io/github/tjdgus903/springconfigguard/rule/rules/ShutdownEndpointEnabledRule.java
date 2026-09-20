package io.github.tjdgus903.springconfigguard.rule.rules;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.model.Severity;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import io.github.tjdgus903.springconfigguard.rule.ConfigRule;

import java.util.Locale;
import java.util.Optional;

/** Detects the Actuator shutdown endpoint being explicitly enabled in production. */
public final class ShutdownEndpointEnabledRule implements ConfigRule {
    public static final String RULE_ID = "SCG013";
    private static final String KEY = "management.endpoint.shutdown.enabled";

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
                "Actuator shutdown endpoint enabled in production",
                "management.endpoint.shutdown.enabled=true enables application shutdown through Actuator when the endpoint is exposed.",
                entry
        ));
    }
}
