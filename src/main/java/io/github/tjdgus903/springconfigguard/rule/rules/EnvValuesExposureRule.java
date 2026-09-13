package io.github.tjdgus903.springconfigguard.rule.rules;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.model.Severity;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import io.github.tjdgus903.springconfigguard.rule.ConfigRule;

import java.util.Locale;
import java.util.Optional;

/** Detects unsanitized Actuator environment values being shown to every user in production. */
public final class EnvValuesExposureRule implements ConfigRule {
    public static final String RULE_ID = "SCG009";
    private static final String KEY = "management.endpoint.env.show-values";

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
                "Actuator environment values are always shown in production",
                "management.endpoint.env.show-values=always can expose unsanitized environment values to every user.",
                entry
        ));
    }
}
