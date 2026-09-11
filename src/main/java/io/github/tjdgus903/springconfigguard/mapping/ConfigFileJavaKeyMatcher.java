package io.github.tjdgus903.springconfigguard.mapping;

import io.github.tjdgus903.springconfigguard.code.ConfigUsage;
import io.github.tjdgus903.springconfigguard.code.ConfigurationPropertyMapping;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Connects parsed configuration entries and Java references by exact property key. */
public final class ConfigFileJavaKeyMatcher {
    public ConfigKeyMappingAnalysis match(
            List<ConfigEntry> configEntries,
            List<ConfigUsage> valueUsages,
            List<ConfigurationPropertyMapping> propertyMappings
    ) {
        Map<String, KeyOccurrences> occurrencesByKey = new LinkedHashMap<>();

        for (ConfigEntry entry : configEntries) {
            occurrencesByKey.computeIfAbsent(entry.key(), ignored -> new KeyOccurrences())
                    .configEntries.add(entry);
        }
        for (ConfigUsage usage : valueUsages) {
            occurrencesByKey.computeIfAbsent(usage.key(), ignored -> new KeyOccurrences())
                    .valueUsages.add(usage);
        }
        for (ConfigurationPropertyMapping mapping : propertyMappings) {
            occurrencesByKey.computeIfAbsent(mapping.key(), ignored -> new KeyOccurrences())
                    .propertyMappings.add(mapping);
        }

        List<ConfigKeyMatch> matches = new ArrayList<>();
        List<ConfigEntry> unmatchedConfigEntries = new ArrayList<>();
        List<ConfigUsage> unmatchedValueUsages = new ArrayList<>();
        List<ConfigurationPropertyMapping> unmatchedPropertyMappings = new ArrayList<>();

        for (Map.Entry<String, KeyOccurrences> keyedOccurrences : occurrencesByKey.entrySet()) {
            String key = keyedOccurrences.getKey();
            KeyOccurrences occurrences = keyedOccurrences.getValue();
            boolean hasConfig = !occurrences.configEntries.isEmpty();
            boolean hasJavaReference = !occurrences.valueUsages.isEmpty()
                    || !occurrences.propertyMappings.isEmpty();

            if (hasConfig && hasJavaReference) {
                matches.add(new ConfigKeyMatch(
                        key,
                        occurrences.configEntries,
                        occurrences.valueUsages,
                        occurrences.propertyMappings
                ));
                continue;
            }

            unmatchedConfigEntries.addAll(occurrences.configEntries);
            unmatchedValueUsages.addAll(occurrences.valueUsages);
            unmatchedPropertyMappings.addAll(occurrences.propertyMappings);
        }

        return new ConfigKeyMappingAnalysis(
                matches,
                unmatchedConfigEntries,
                unmatchedValueUsages,
                unmatchedPropertyMappings
        );
    }

    private static final class KeyOccurrences {
        private final List<ConfigEntry> configEntries = new ArrayList<>();
        private final List<ConfigUsage> valueUsages = new ArrayList<>();
        private final List<ConfigurationPropertyMapping> propertyMappings = new ArrayList<>();
    }
}
