package io.github.tjdgus903.springconfigguard.scanner;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Detects Spring Boot profile names and whether they should be treated as production. */
public final class ConfigProfileDetector {
    private static final Set<String> DEFAULT_PRODUCTION_ALIASES = Set.of("prod", "production", "prd");

    private final Set<String> productionAliases;

    public ConfigProfileDetector() {
        this(DEFAULT_PRODUCTION_ALIASES);
    }

    public ConfigProfileDetector(Collection<String> productionAliases) {
        this.productionAliases = productionAliases.stream()
                .map(ConfigProfileDetector::normalize)
                .filter(alias -> !alias.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public Optional<ConfigProfile> detect(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return Optional.empty();
        }

        String fileName = fileName(filePath);
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        String extension = supportedExtension(lowerName);
        if (extension == null) {
            return Optional.empty();
        }

        String baseName = fileName.substring(0, fileName.length() - extension.length());
        String lowerBaseName = baseName.toLowerCase(Locale.ROOT);

        if ("application".equals(lowerBaseName)) {
            return Optional.of(new ConfigProfile("default", false));
        }

        String prefix = "application-";
        if (!lowerBaseName.startsWith(prefix) || baseName.length() <= prefix.length()) {
            return Optional.empty();
        }

        String profile = baseName.substring(prefix.length());
        boolean production = productionAliases.contains(normalize(profile));
        return Optional.of(new ConfigProfile(profile, production));
    }

    private static String fileName(String filePath) {
        String normalized = filePath.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private static String supportedExtension(String lowerName) {
        if (lowerName.endsWith(".properties")) {
            return ".properties";
        }
        if (lowerName.endsWith(".yaml")) {
            return ".yaml";
        }
        if (lowerName.endsWith(".yml")) {
            return ".yml";
        }
        return null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
