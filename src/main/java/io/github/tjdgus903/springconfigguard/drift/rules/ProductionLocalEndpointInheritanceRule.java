package io.github.tjdgus903.springconfigguard.drift.rules;

import io.github.tjdgus903.springconfigguard.drift.EffectiveConfigValue;
import io.github.tjdgus903.springconfigguard.drift.ProfileDriftFinding;
import io.github.tjdgus903.springconfigguard.drift.ProfileDriftKind;
import io.github.tjdgus903.springconfigguard.drift.ProfileDriftRule;
import io.github.tjdgus903.springconfigguard.model.Severity;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Detects a production profile inheriting an endpoint-like value that still points at the local
 * machine. This is intentionally narrow to reduce false positives in the first Pro rule.
 */
public final class ProductionLocalEndpointInheritanceRule implements ProfileDriftRule {
    public static final String RULE_ID = "SCG-PD001";

    private static final Pattern LOCALHOST = Pattern.compile(
            "(?i)(^|[^a-z0-9.-])localhost(?=[:/?#]|$)"
    );
    private static final Pattern IPV4_LOOPBACK = Pattern.compile(
            "(^|[^0-9])127\\.0\\.0\\.1(?=[:/?#]|$)"
    );
    private static final Pattern IPV6_LOOPBACK = Pattern.compile(
            "(^|[^0-9a-f:])(?:\\[::1\\]|::1)(?=[:/?#]|$)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public Optional<ProfileDriftFinding> check(
            EffectiveConfigValue value,
            boolean productionProfile
    ) {
        if (!productionProfile || !value.inherited() || !"default".equalsIgnoreCase(value.sourceProfile())) {
            return Optional.empty();
        }
        if (value.value() == null || !isEndpointLikeKey(value.key()) || !containsLocalEndpoint(value.value())) {
            return Optional.empty();
        }

        return Optional.of(new ProfileDriftFinding(
                RULE_ID,
                ProfileDriftKind.RISK,
                Severity.HIGH,
                value.key(),
                value.profile(),
                "Production profile inherits a local development endpoint",
                "The production profile does not override '" + value.key()
                        + "', so it inherits '" + value.value() + "' from the default configuration.",
                value
        ));
    }

    private boolean isEndpointLikeKey(String key) {
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".url")
                || normalized.endsWith(".uri")
                || normalized.endsWith(".host")
                || normalized.endsWith(".hostname")
                || normalized.endsWith(".endpoint")
                || normalized.endsWith(".base-url")
                || normalized.endsWith(".baseurl");
    }

    private boolean containsLocalEndpoint(String rawValue) {
        return LOCALHOST.matcher(rawValue).find()
                || IPV4_LOOPBACK.matcher(rawValue).find()
                || IPV6_LOOPBACK.matcher(rawValue).find();
    }
}
