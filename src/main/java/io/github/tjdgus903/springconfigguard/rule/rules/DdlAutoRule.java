package io.github.tjdgus903.springconfigguard.rule.rules;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.model.Severity;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import io.github.tjdgus903.springconfigguard.rule.ConfigRule;

import java.util.Locale;
import java.util.Optional;

public final class DdlAutoRule implements ConfigRule {
    public static final String RULE_ID = "SCG001";
    private static final String KEY = "spring.jpa.hibernate.ddl-auto";

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public Optional<Finding> check(ConfigEntry entry, ConfigContext context) {
        if (!context.productionProfile() || !KEY.equals(entry.key()) || entry.value() == null) {
            return Optional.empty();
        }

        String value = entry.value().trim().toLowerCase(Locale.ROOT);
        Severity severity = switch (value) {
            case "create", "create-drop" -> Severity.CRITICAL;
            case "update" -> Severity.WARNING;
            default -> null;
        };

        if (severity == null) {
            return Optional.empty();
        }

        return Optional.of(new Finding(
                RULE_ID,
                severity,
                "Risky Hibernate schema management setting in production",
                "spring.jpa.hibernate.ddl-auto=" + value + " can modify the database schema during application startup.",
                entry
        ));
    }
}
