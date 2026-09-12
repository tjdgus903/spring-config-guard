package io.github.tjdgus903.springconfigguard.diff;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/** Compares parsed local configuration entries without any VCS or IntelliJ dependency. */
public final class ConfigEntryDiffAnalyzer {
    public ConfigDiffAnalysis analyze(List<ConfigEntry> before, List<ConfigEntry> after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");

        Map<EntryIdentity, List<ConfigEntry>> beforeByIdentity = groupByIdentity(before);
        Map<EntryIdentity, List<ConfigEntry>> afterByIdentity = groupByIdentity(after);
        TreeSet<EntryIdentity> identities = new TreeSet<>();
        identities.addAll(beforeByIdentity.keySet());
        identities.addAll(afterByIdentity.keySet());

        List<ConfigChange> changes = new ArrayList<>();
        for (EntryIdentity identity : identities) {
            compareOccurrences(
                    beforeByIdentity.getOrDefault(identity, List.of()),
                    afterByIdentity.getOrDefault(identity, List.of()),
                    changes
            );
        }
        return new ConfigDiffAnalysis(changes);
    }

    private static Map<EntryIdentity, List<ConfigEntry>> groupByIdentity(List<ConfigEntry> entries) {
        Map<EntryIdentity, List<ConfigEntry>> grouped = new TreeMap<>();
        for (ConfigEntry entry : entries) {
            ConfigEntry nonNullEntry = Objects.requireNonNull(entry, "entries must not contain null");
            grouped.computeIfAbsent(EntryIdentity.from(nonNullEntry), ignored -> new ArrayList<>())
                    .add(nonNullEntry);
        }
        return grouped;
    }

    private static void compareOccurrences(
            List<ConfigEntry> before,
            List<ConfigEntry> after,
            List<ConfigChange> changes
    ) {
        int count = Math.max(before.size(), after.size());
        for (int index = 0; index < count; index++) {
            ConfigEntry previous = index < before.size() ? before.get(index) : null;
            ConfigEntry current = index < after.size() ? after.get(index) : null;
            if (previous == null) {
                changes.add(new ConfigChange(ConfigChangeKind.ADDED, null, current));
            } else if (current == null) {
                changes.add(new ConfigChange(ConfigChangeKind.REMOVED, previous, null));
            } else if (!Objects.equals(previous.value(), current.value())) {
                changes.add(new ConfigChange(ConfigChangeKind.MODIFIED, previous, current));
            }
        }
    }

    private record EntryIdentity(String filePath, String profile, String key) implements Comparable<EntryIdentity> {
        private static EntryIdentity from(ConfigEntry entry) {
            return new EntryIdentity(
                    Objects.requireNonNull(entry.filePath(), "filePath"),
                    Objects.requireNonNull(entry.profile(), "profile"),
                    Objects.requireNonNull(entry.key(), "key")
            );
        }

        @Override
        public int compareTo(EntryIdentity other) {
            int fileComparison = filePath.compareTo(other.filePath);
            if (fileComparison != 0) {
                return fileComparison;
            }
            int profileComparison = profile.compareTo(other.profile);
            return profileComparison != 0 ? profileComparison : key.compareTo(other.key);
        }
    }
}
