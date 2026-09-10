package io.github.tjdgus903.springconfigguard.rule.rules;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.model.Severity;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import io.github.tjdgus903.springconfigguard.rule.ConfigRule;

import java.util.Arrays;
import java.util.Optional;

/** Detects wildcard Actuator web endpoint exposure in production. */
public final class ActuatorWildcardExposureRule implements ConfigRule {
    public static final String RULE_ID = "SCG002";
    private static final String KEY = "management.endpoints.web.exposure.include";

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public Optional<Finding> check(ConfigEntry entry, ConfigContext context) {
        if (!context.productionProfile() || !KEY.equals(entry.key()) || entry.value() == null) {
            return Optional.empty();
        }

        boolean wildcard = Arrays.stream(entry.value().split(","))
                .map(String::trim)
                .anyMatch("*"::equals);
        if (!wildcard) {
            return Optional.empty();
        }

        return Optional.of(new Finding(
                RULE_ID,
                Severity.HIGH,
                "Wildcard Actuator endpoint exposure in production",
                "management.endpoints.web.exposure.include contains '*', which can expose every available web endpoint unless separately restricted.",
                entry
        ));
    }
}
