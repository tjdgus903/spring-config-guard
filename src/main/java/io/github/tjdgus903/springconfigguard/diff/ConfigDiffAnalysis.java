package io.github.tjdgus903.springconfigguard.diff;

import java.util.List;

/** Immutable result of comparing two local configuration entry inventories. */
public record ConfigDiffAnalysis(List<ConfigChange> changes) {
    public ConfigDiffAnalysis {
        changes = List.copyOf(changes);
    }

    public List<ConfigChange> additions() {
        return changes.stream().filter(change -> change.kind() == ConfigChangeKind.ADDED).toList();
    }

    public List<ConfigChange> modifications() {
        return changes.stream().filter(change -> change.kind() == ConfigChangeKind.MODIFIED).toList();
    }

    public List<ConfigChange> removals() {
        return changes.stream().filter(change -> change.kind() == ConfigChangeKind.REMOVED).toList();
    }
}
