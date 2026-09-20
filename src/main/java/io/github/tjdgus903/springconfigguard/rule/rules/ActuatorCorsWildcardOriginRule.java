package io.github.tjdgus903.springconfigguard.rule.rules;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.model.Severity;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import io.github.tjdgus903.springconfigguard.rule.ConfigRule;

import java.util.Arrays;
import java.util.Optional;

/** Detects wildcard Actuator CORS origins in production. */
public final class ActuatorCorsWildcardOriginRule implements ConfigRule {
    public static final String RULE_ID = "SCG015";
    private static final String KEY = "management.endpoints.web.cors.allowed-origins";

    @Override public String id() { return RULE_ID; }

    @Override
    public Optional<Finding> check(ConfigEntry entry, ConfigContext context) {
        if (!context.productionProfile() || !KEY.equals(entry.key()) || entry.value() == null) return Optional.empty();
        boolean wildcard = Arrays.stream(entry.value().split(","))
                .map(String::trim)
                .anyMatch("*"::equals);
        if (!wildcard) return Optional.empty();
        return Optional.of(new Finding(
                RULE_ID,
                Severity.WARNING,
                "Wildcard Actuator CORS origin configured in production",
                "management.endpoints.web.cors.allowed-origins contains '*', allowing requests from any origin when Actuator CORS is enabled.",
                entry
        ));
    }
}