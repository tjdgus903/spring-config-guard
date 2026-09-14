package io.github.tjdgus903.springconfigguard.rule.rules;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.model.Severity;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import io.github.tjdgus903.springconfigguard.rule.ConfigRule;

import java.util.Locale;
import java.util.Optional;

/** Detects Actuator health components being shown to every user in production. */
public final class HealthComponentsExposureRule implements ConfigRule {
    public static final String RULE_ID = "SCG012";
    private static final String KEY = "management.endpoint.health.show-components";

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public Optional<Finding> check(ConfigEntry entry, ConfigContext context) {
        if (!context.productionProfile() || !KEY.equals(entry.key()) || entry.value() == null) {
            return Optional.empty();
        }
        if (!"always".equals(entry.value().trim().toLowerCase(Locale.ROOT))) {
            return Optional.empty();
        }
        return Optional.of(new Finding(
                RULE_ID,
                Severity.HIGH,
                "Actuator health components are always shown in production",
                "management.endpoint.health.show-components=always exposes component health information to every user.",
                entry
        ));
    }
}
