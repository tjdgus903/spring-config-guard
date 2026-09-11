package io.github.tjdgus903.springconfigguard.project;

import io.github.tjdgus903.springconfigguard.code.ConfigUsage;
import io.github.tjdgus903.springconfigguard.code.ConfigurationPropertyMapping;

import java.util.List;

/** Immutable Java reference snapshot; no PSI objects escape project collection. */
public record ProjectJavaConfigReferences(
        List<ConfigUsage> valueUsages,
        List<ConfigurationPropertyMapping> propertyMappings
) {
    public ProjectJavaConfigReferences {
        valueUsages = List.copyOf(valueUsages);
        propertyMappings = List.copyOf(propertyMappings);
    }
}
