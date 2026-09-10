package io.github.tjdgus903.springconfigguard.scanner;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;

import java.util.List;
import java.util.Locale;

/** Entry point for deterministic parsing of Spring Boot configuration files. */
public final class ConfigFileScanner {
    private final YamlConfigScanner yamlScanner = new YamlConfigScanner();
    private final PropertiesConfigScanner propertiesScanner = new PropertiesConfigScanner();

    public List<ConfigEntry> scan(String content, String filePath, String profile) {
        String normalizedPath = filePath.toLowerCase(Locale.ROOT);
        if (normalizedPath.endsWith(".yml") || normalizedPath.endsWith(".yaml")) {
            return yamlScanner.scan(content, filePath, profile);
        }
        if (normalizedPath.endsWith(".properties")) {
            return propertiesScanner.scan(content, filePath, profile);
        }
        throw new IllegalArgumentException("Unsupported Spring configuration file: " + filePath);
    }
}
