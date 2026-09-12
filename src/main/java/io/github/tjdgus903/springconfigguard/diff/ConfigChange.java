package io.github.tjdgus903.springconfigguard.diff;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;

import java.util.Objects;

/** A local configuration change. Values are retained only for deterministic rule evaluation. */
public record ConfigChange(ConfigChangeKind kind, ConfigEntry before, ConfigEntry after) {
    public ConfigChange {
        Objects.requireNonNull(kind, "kind");
        switch (kind) {
            case ADDED -> {
                if (before != null || after == null) {
                    throw new IllegalArgumentException("An added change requires only an after entry");
                }
            }
            case MODIFIED -> {
                if (before == null || after == null) {
                    throw new IllegalArgumentException("A modified change requires before and after entries");
                }
            }
            case REMOVED -> {
                if (before == null || after != null) {
                    throw new IllegalArgumentException("A removed change requires only a before entry");
                }
            }
        }
    }

    /** Returns the entry whose new value can be analyzed; removals have no current entry. */
    public ConfigEntry currentEntry() {
        return after;
    }
}
