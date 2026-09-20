package io.github.tjdgus903.springconfigguard.rule;

import java.util.Map;
import java.util.Objects;

public final class RuleGuidanceCatalog {
    private static final Map<String, String> GUIDANCE = Map.ofEntries(
        Map.entry("SCG001", "Use validate or none in production; apply schema changes through reviewed migrations."),
        Map.entry("SCG002", "Expose only required Actuator endpoints and protect them with authentication."),
        Map.entry("SCG003", "Disable stack traces in production responses and log details securely on the server."),
        Map.entry("SCG004", "Keep root logging at INFO or higher in production unless a controlled diagnostic window is active."),
        Map.entry("SCG005", "Disable Hibernate SQL logging in production or route it to a controlled, access-restricted logger."),
        Map.entry("SCG006", "Return generic production error messages and keep internal details in protected server logs."),
        Map.entry("SCG007", "Avoid exposing binding errors to clients; return a generic validation response."),
        Map.entry("SCG008", "Disable the H2 console outside local development."),
        Map.entry("SCG009", "Use never or when-authorized for Actuator environment value sanitization in production."),
        Map.entry("SCG010", "Use never or when-authorized for Actuator configprops value sanitization in production."),
        Map.entry("SCG011", "Use never or when-authorized for Actuator health details in production."),
        Map.entry("SCG012", "Use never or when-authorized for Actuator health components in production."),
        Map.entry("SCG013", "Keep the Actuator shutdown endpoint disabled in production unless a reviewed operational requirement and access controls justify it.")
    );

    private RuleGuidanceCatalog() {}

    public static String guidance(String ruleId) {
        return GUIDANCE.getOrDefault(Objects.requireNonNull(ruleId), "Review this production setting and choose the least-exposing safe value.");
    }
}
