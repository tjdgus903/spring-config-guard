package io.github.tjdgus903.springconfigguard.mapping;

import io.github.tjdgus903.springconfigguard.code.ConfigUsage;
import io.github.tjdgus903.springconfigguard.code.ConfigurationPropertyMapping;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;

import java.util.List;

/** All configuration-file and Java-source occurrences connected by one exact property key. */
public record ConfigKeyMatch(
        String key,
        List<ConfigEntry> configEntries,
        List<ConfigUsage> valueUsages,
        List<ConfigurationPropertyMapping> propertyMappings
) {
    public ConfigKeyMatch {
        configEntries = List.copyOf(configEntries);
        valueUsages = List.copyOf(valueUsages);
        propertyMappings = List.copyOf(propertyMappings);
    }
}
