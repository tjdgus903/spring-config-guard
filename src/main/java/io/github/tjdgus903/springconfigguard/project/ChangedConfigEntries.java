package io.github.tjdgus903.springconfigguard.project;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;

import java.util.List;

/** Parsed local configuration entries from the two sides of a VCS change list. */
public record ChangedConfigEntries(List<ConfigEntry> before, List<ConfigEntry> after) {
    public ChangedConfigEntries {
        before = List.copyOf(before);
        after = List.copyOf(after);
    }
}
