package io.github.tjdgus903.springconfigguard.mapping;

import io.github.tjdgus903.springconfigguard.code.ConfigUsage;
import io.github.tjdgus903.springconfigguard.code.ConfigurationPropertyMapping;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;

import java.util.List;

/** Deterministic result of connecting configuration-file keys to Java-source references. */
public record ConfigKeyMappingAnalysis(
        List<ConfigKeyMatch> matches,
        List<ConfigEntry> unmatchedConfigEntries,
        List<ConfigUsage> unmatchedValueUsages,
        List<ConfigurationPropertyMapping> unmatchedPropertyMappings
) {
    public ConfigKeyMappingAnalysis {
        matches = List.copyOf(matches);
        unmatchedConfigEntries = List.copyOf(unmatchedConfigEntries);
        unmatchedValueUsages = List.copyOf(unmatchedValueUsages);
        unmatchedPropertyMappings = List.copyOf(unmatchedPropertyMappings);
    }

    /** Unmatched literal {@code @Value} references that declare no fallback value. */
    public List<ConfigUsage> unmatchedValueUsagesWithoutDefault() {
        return unmatchedValueUsages.stream()
                .filter(usage -> usage.defaultValue() == null)
                .toList();
    }

    /** Unmatched literal {@code @Value} references that declare a fallback, including an empty one. */
    public List<ConfigUsage> unmatchedValueUsagesWithDefault() {
        return unmatchedValueUsages.stream()
                .filter(usage -> usage.defaultValue() != null)
                .toList();
    }
}
