package io.github.tjdgus903.springconfigguard.mapping;

import io.github.tjdgus903.springconfigguard.code.ConfigUsage;
import io.github.tjdgus903.springconfigguard.code.ConfigurationPropertyMapping;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Bounded local inventory report. Property values and placeholder/default text are never rendered. */
public final class ConfigKeyMappingReportFormatter {
    private static final int MAX_ITEMS_PER_SECTION = 20;
    private static final int MAX_LOCATIONS_PER_KIND = 5;
    private static final int MAX_LABEL_LENGTH = 240;
    private static final Comparator<ConfigEntry> CONFIG_ORDER = Comparator.comparing(ConfigEntry::key)
            .thenComparing(ConfigEntry::profile).thenComparing(ConfigEntry::filePath).thenComparingInt(ConfigEntry::line);
    private static final Comparator<ConfigUsage> VALUE_ORDER = Comparator.comparing(ConfigUsage::key)
            .thenComparing(ConfigUsage::filePath).thenComparingInt(ConfigUsage::line);
    private static final Comparator<ConfigurationPropertyMapping> PROPERTY_ORDER =
            Comparator.comparing(ConfigurationPropertyMapping::key)
                    .thenComparing(ConfigurationPropertyMapping::filePath)
                    .thenComparingInt(ConfigurationPropertyMapping::line);

    public String format(ConfigKeyMappingAnalysis analysis) {
        StringBuilder report = new StringBuilder("Config / Java Key Mapping\n\n");
        report.append("Matched keys: ").append(analysis.matches().size()).append('\n')
                .append("Config entries without an exact Java reference: ")
                .append(analysis.unmatchedConfigEntries().size()).append('\n')
                .append("@Value references without an exact config entry: ")
                .append(analysis.unmatchedValueUsages().size()).append('\n')
                .append("@ConfigurationProperties fields without an exact config entry: ")
                .append(analysis.unmatchedPropertyMappings().size()).append("\n\n")
                .append("Scope: exact-key inventory across project modules and profiles, including test sources.\n")
                .append("Unmatched occurrences are informational, not missing/unused-key warnings.\n")
                .append("Framework binding, environment values and external configuration are not resolved.\n")
                .append("Configuration values and @Value default text are omitted.\n");

        if (analysis.matches().isEmpty() && analysis.unmatchedConfigEntries().isEmpty()
                && analysis.unmatchedValueUsages().isEmpty() && analysis.unmatchedPropertyMappings().isEmpty()) {
            return report.append("\nNo supported configuration entries or Java references were found.").toString();
        }

        if (!analysis.matches().isEmpty()) {
            report.append("\nMatched keys:\n");
            appendLimited(report, analysis.matches().stream().sorted(Comparator.comparing(ConfigKeyMatch::key)).toList(),
                    MAX_ITEMS_PER_SECTION, match -> {
                        report.append("- ").append(label(match.key())).append('\n');
                        appendLimited(report, match.configEntries().stream().sorted(CONFIG_ORDER).toList(),
                                MAX_LOCATIONS_PER_KIND, entry -> appendConfigLocation(report, entry));
                        appendLimited(report, match.valueUsages().stream().sorted(VALUE_ORDER).toList(),
                                MAX_LOCATIONS_PER_KIND, usage -> appendValueLocation(report, usage));
                        appendLimited(report, match.propertyMappings().stream().sorted(PROPERTY_ORDER).toList(),
                                MAX_LOCATIONS_PER_KIND, mapping -> appendPropertyLocation(report, mapping));
                    });
        }

        if (!analysis.unmatchedConfigEntries().isEmpty()) {
            report.append("\nConfig entries without an exact Java reference:\n");
            appendLimited(report, analysis.unmatchedConfigEntries().stream().sorted(CONFIG_ORDER).toList(),
                    MAX_ITEMS_PER_SECTION, entry -> {
                        report.append("- ").append(label(entry.key())).append('\n');
                        appendConfigLocation(report, entry);
                    });
        }
        if (!analysis.unmatchedValueUsages().isEmpty()) {
            report.append("\n@Value references without an exact config entry:\n");
            appendLimited(report, analysis.unmatchedValueUsages().stream().sorted(VALUE_ORDER).toList(),
                    MAX_ITEMS_PER_SECTION, usage -> {
                        report.append("- ").append(label(usage.key())).append('\n');
                        appendValueLocation(report, usage);
                    });
        }
        if (!analysis.unmatchedPropertyMappings().isEmpty()) {
            report.append("\n@ConfigurationProperties fields without an exact config entry:\n");
            appendLimited(report, analysis.unmatchedPropertyMappings().stream().sorted(PROPERTY_ORDER).toList(),
                    MAX_ITEMS_PER_SECTION, mapping -> {
                        report.append("- ").append(label(mapping.key())).append('\n');
                        appendPropertyLocation(report, mapping);
                    });
        }
        return report.toString().stripTrailing();
    }

    private static void appendConfigLocation(StringBuilder report, ConfigEntry entry) {
        report.append("  config [").append(label(entry.profile())).append("] ")
                .append(location(entry.filePath(), entry.line())).append('\n');
    }

    private static void appendValueLocation(StringBuilder report, ConfigUsage usage) {
        report.append("  @Value ").append(location(usage.filePath(), usage.line()))
                .append(usage.defaultValue() == null ? "" : " (default present)").append('\n');
    }

    private static void appendPropertyLocation(StringBuilder report, ConfigurationPropertyMapping mapping) {
        report.append("  @ConfigurationProperties ").append(location(mapping.filePath(), mapping.line()))
                .append(" :: ").append(label(mapping.declaringClass())).append('#')
                .append(label(mapping.fieldName())).append('\n');
    }

    private static <T> void appendLimited(StringBuilder report, List<T> items, int limit, Consumer<T> append) {
        int visible = Math.min(limit, items.size());
        for (int i = 0; i < visible; i++) {
            append.accept(items.get(i));
        }
        if (items.size() > visible) {
            report.append("  ... ").append(items.size() - visible).append(" more omitted\n");
        }
    }

    private static String location(String path, int line) {
        return label(path) + ":" + line;
    }

    private static String label(String text) {
        String singleLine = text.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        return singleLine.length() <= MAX_LABEL_LENGTH
                ? singleLine : singleLine.substring(0, MAX_LABEL_LENGTH) + "...";
    }
}
