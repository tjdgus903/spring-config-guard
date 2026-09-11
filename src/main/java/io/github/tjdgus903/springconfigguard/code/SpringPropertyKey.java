package io.github.tjdgus903.springconfigguard.code;

import java.util.Locale;
import java.util.Objects;

/**
 * Pure Java helpers for deriving Spring-style property keys from Java field names.
 *
 * <p>This class intentionally has no IntelliJ or Spring runtime dependency so the same key
 * derivation can be reused by IDE, CLI, and future commit-guard adapters.</p>
 */
public final class SpringPropertyKey {
    private SpringPropertyKey() {
    }

    /**
     * Converts a Java-style identifier to lowercase kebab case.
     *
     * <p>Acronym boundaries are preserved in the common form, for example
     * {@code URLValue -> url-value} and {@code SSLConfig -> ssl-config}.</p>
     */
    public static String toKebabCase(String javaName) {
        Objects.requireNonNull(javaName, "javaName");
        if (javaName.isBlank()) {
            throw new IllegalArgumentException("javaName must not be blank");
        }

        StringBuilder result = new StringBuilder(javaName.length() + 8);
        for (int i = 0; i < javaName.length(); i++) {
            char current = javaName.charAt(i);

            if (current == '_' || current == '-') {
                appendSeparator(result);
                continue;
            }

            if (!Character.isLetterOrDigit(current)) {
                appendSeparator(result);
                continue;
            }

            if (Character.isUpperCase(current) && shouldStartWord(javaName, i)) {
                appendSeparator(result);
            }

            result.append(String.valueOf(current).toLowerCase(Locale.ROOT));
        }

        trimTrailingSeparator(result);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("javaName must contain a letter or digit");
        }
        return result.toString();
    }

    /** Builds {@code prefix.field-name}; an empty prefix returns only the derived field key. */
    public static String withPrefix(String prefix, String javaFieldName) {
        String fieldKey = toKebabCase(javaFieldName);
        if (prefix == null || prefix.isBlank()) {
            return fieldKey;
        }

        String normalizedPrefix = prefix.trim();
        while (normalizedPrefix.endsWith(".")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }
        if (normalizedPrefix.isBlank()) {
            return fieldKey;
        }
        return normalizedPrefix + "." + fieldKey;
    }

    private static boolean shouldStartWord(String value, int index) {
        if (index == 0) {
            return false;
        }

        char previous = value.charAt(index - 1);
        if (previous == '_' || previous == '-') {
            return false;
        }

        if (Character.isLowerCase(previous) || Character.isDigit(previous)) {
            return true;
        }

        if (Character.isUpperCase(previous) && index + 1 < value.length()) {
            return Character.isLowerCase(value.charAt(index + 1));
        }

        return false;
    }

    private static void appendSeparator(StringBuilder result) {
        if (!result.isEmpty() && result.charAt(result.length() - 1) != '-') {
            result.append('-');
        }
    }

    private static void trimTrailingSeparator(StringBuilder result) {
        while (!result.isEmpty() && result.charAt(result.length() - 1) == '-') {
            result.deleteCharAt(result.length() - 1);
        }
    }
}
