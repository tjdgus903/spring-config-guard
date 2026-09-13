package io.github.tjdgus903.springconfigguard.rule.rules;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.model.Severity;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import io.github.tjdgus903.springconfigguard.rule.ConfigRule;

import java.util.Locale;
import java.util.Optional;

/** Detects unconditional error-message exposure in production responses. */
public final class ErrorMessageExposureRule implements ConfigRule {
    public static final String RULE_ID = "SCG006";
    private static final String KEY = "server.error.include-message";

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
                "Error messages are always exposed in production",
                "server.error.include-message=always may disclose internal exception messages in error responses.",
                entry
        ));
    }
}
