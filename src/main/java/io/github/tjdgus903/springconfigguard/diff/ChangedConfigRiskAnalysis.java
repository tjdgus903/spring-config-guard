package io.github.tjdgus903.springconfigguard.diff;

import java.util.List;

/** Immutable deterministic risk findings for the current values in a local configuration diff. */
public record ChangedConfigRiskAnalysis(List<ChangedConfigRiskFinding> findings) {
    public ChangedConfigRiskAnalysis {
        findings = List.copyOf(findings);
    }
}
