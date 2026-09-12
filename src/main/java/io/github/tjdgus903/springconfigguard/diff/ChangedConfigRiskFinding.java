package io.github.tjdgus903.springconfigguard.diff;

import io.github.tjdgus903.springconfigguard.model.Finding;

import java.util.Objects;

/** A deterministic risk finding paired with the added or modified local configuration change it came from. */
public record ChangedConfigRiskFinding(ConfigChange change, Finding finding) {
    public ChangedConfigRiskFinding {
        Objects.requireNonNull(change, "change");
        Objects.requireNonNull(finding, "finding");
        if (change.currentEntry() == null || !change.currentEntry().equals(finding.entry())) {
            throw new IllegalArgumentException("A changed-config finding must refer to the change's current entry");
        }
    }
}
