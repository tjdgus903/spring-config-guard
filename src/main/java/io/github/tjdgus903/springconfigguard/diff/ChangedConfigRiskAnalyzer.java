package io.github.tjdgus903.springconfigguard.diff;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import io.github.tjdgus903.springconfigguard.rule.MvpRuleRegistry;
import io.github.tjdgus903.springconfigguard.rule.RuleEngine;
import io.github.tjdgus903.springconfigguard.scanner.ConfigProfileDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure deterministic evaluation of existing rules for the new side of local configuration changes.
 * VCS access, UI rendering, and commit enforcement deliberately remain outside this core.
 */
public final class ChangedConfigRiskAnalyzer {
    private final RuleEngine ruleEngine;
    private final ConfigProfileDetector profileDetector;

    public ChangedConfigRiskAnalyzer() {
        this(MvpRuleRegistry.ruleEngine(), new ConfigProfileDetector());
    }

    ChangedConfigRiskAnalyzer(RuleEngine ruleEngine, ConfigProfileDetector profileDetector) {
        this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine");
        this.profileDetector = Objects.requireNonNull(profileDetector, "profileDetector");
    }

    public ChangedConfigRiskAnalysis analyze(ConfigDiffAnalysis diff) {
        Objects.requireNonNull(diff, "diff");

        List<ChangedConfigRiskFinding> findings = new ArrayList<>();
        for (ConfigChange change : diff.changes()) {
            ConfigEntry currentEntry = change.currentEntry();
            if (currentEntry == null) {
                continue;
            }
            ConfigContext context = new ConfigContext(profileDetector.isProductionProfile(currentEntry.profile()));
            for (Finding finding : ruleEngine.analyze(currentEntry, context)) {
                findings.add(new ChangedConfigRiskFinding(change, finding));
            }
        }
        return new ChangedConfigRiskAnalysis(findings);
    }
}
