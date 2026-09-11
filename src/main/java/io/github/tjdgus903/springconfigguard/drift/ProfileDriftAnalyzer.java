package io.github.tjdgus903.springconfigguard.drift;

import io.github.tjdgus903.springconfigguard.drift.rules.ProductionLocalEndpointInheritanceRule;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Severity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Resolves default Spring configuration into named profiles and reports both ordinary drift and
 * rule-backed risks. The input order defines precedence for duplicate profile/key entries; later
 * entries win.
 */
public final class ProfileDriftAnalyzer {
    private static final Set<String> DEFAULT_PRODUCTION_ALIASES = Set.of("prod", "production", "prd");

    private final Set<String> productionAliases;
    private final List<ProfileDriftRule> rules;

    public ProfileDriftAnalyzer() {
        this(DEFAULT_PRODUCTION_ALIASES, List.of(new ProductionLocalEndpointInheritanceRule()));
    }

    public ProfileDriftAnalyzer(Collection<String> productionAliases, List<ProfileDriftRule> rules) {
        this.productionAliases = productionAliases.stream()
                .map(ProfileDriftAnalyzer::normalizeProfile)
                .filter(profile -> !profile.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        this.rules = List.copyOf(rules);
    }

    public ProfileDriftAnalysis analyze(List<ConfigEntry> entries) {
        Map<String, Map<String, ConfigEntry>> byProfile = indexByProfile(entries);
        Map<String, ConfigEntry> defaults = byProfile.getOrDefault("default", Map.of());

        List<EffectiveConfigValue> effectiveValues = new ArrayList<>();
        List<ProfileDriftFinding> findings = new ArrayList<>();

        defaults.keySet().stream().sorted().forEach(key -> {
            ConfigEntry entry = defaults.get(key);
            effectiveValues.add(toEffective("default", entry, false, entry));
        });

        Set<String> targetProfiles = new TreeSet<>(byProfile.keySet());
        targetProfiles.remove("default");

        for (String profile : targetProfiles) {
            Map<String, ConfigEntry> profileEntries = byProfile.get(profile);
            Set<String> keys = new TreeSet<>();
            keys.addAll(defaults.keySet());
            keys.addAll(profileEntries.keySet());

            boolean production = productionAliases.contains(profile);
            for (String key : keys) {
                ConfigEntry explicit = profileEntries.get(key);
                ConfigEntry fallback = defaults.get(key);
                ConfigEntry source = explicit != null ? explicit : fallback;
                if (source == null) {
                    continue;
                }

                EffectiveConfigValue effective = toEffective(profile, source, explicit == null, source);
                effectiveValues.add(effective);

                if (explicit != null && (fallback == null || !Objects.equals(explicit.value(), fallback.value()))) {
                    findings.add(new ProfileDriftFinding(
                            null,
                            ProfileDriftKind.DIFFERENCE,
                            Severity.INFO,
                            key,
                            profile,
                            "Profile value differs from default configuration",
                            fallback == null
                                    ? "The key exists only in profile '" + profile + "'."
                                    : "Default value '" + fallback.value() + "' is overridden by '"
                                            + explicit.value() + "' in profile '" + profile + "'.",
                            effective
                    ));
                }

                for (ProfileDriftRule rule : rules) {
                    rule.check(effective, production).ifPresent(findings::add);
                }
            }
        }

        return new ProfileDriftAnalysis(effectiveValues, findings);
    }

    private Map<String, Map<String, ConfigEntry>> indexByProfile(List<ConfigEntry> entries) {
        Map<String, Map<String, ConfigEntry>> byProfile = new LinkedHashMap<>();
        for (ConfigEntry entry : entries) {
            if (entry == null || entry.key() == null || entry.key().isBlank()) {
                continue;
            }
            String profile = normalizeProfile(entry.profile());
            byProfile.computeIfAbsent(profile, ignored -> new LinkedHashMap<>())
                    .put(entry.key(), entry);
        }
        return byProfile;
    }

    private EffectiveConfigValue toEffective(
            String targetProfile,
            ConfigEntry source,
            boolean inherited,
            ConfigEntry sourceEntry
    ) {
        return new EffectiveConfigValue(
                source.key(),
                targetProfile,
                source.value(),
                inherited,
                normalizeProfile(sourceEntry.profile()),
                sourceEntry.filePath(),
                sourceEntry.line()
        );
    }

    private static String normalizeProfile(String profile) {
        if (profile == null || profile.isBlank()) {
            return "default";
        }
        return profile.trim().toLowerCase(Locale.ROOT);
    }
}
