package io.github.tjdgus903.springconfigguard.diff;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Formats local changed-configuration risk metadata without rendering configuration values. */
public final class ChangedConfigRiskReportFormatter {
    private static final int MAX_FINDINGS = 20;

    public String format(ConfigDiffAnalysis diff, ChangedConfigRiskAnalysis riskAnalysis) {
        Objects.requireNonNull(diff, "diff");
        Objects.requireNonNull(riskAnalysis, "riskAnalysis");

        if (diff.changes().isEmpty()) {
            return "No local Spring Boot application configuration changes were found.";
        }

        StringBuilder report = new StringBuilder();
        report.append("Changed Configuration Analysis\n\n")
                .append("Changed entries: ").append(diff.changes().size()).append('\n')
                .append("Added: ").append(diff.additions().size()).append('\n')
                .append("Modified: ").append(diff.modifications().size()).append('\n')
                .append("Removed: ").append(diff.removals().size()).append('\n')
                .append("Deterministic risk findings: ").append(riskAnalysis.findings().size()).append('\n');

        if (riskAnalysis.findings().isEmpty()) {
            return report.append("\nNo deterministic risk findings detected.").toString();
        }

        List<ChangedConfigRiskFinding> findings = riskAnalysis.findings().stream()
                .sorted(Comparator
                        .comparing((ChangedConfigRiskFinding item) -> item.finding().severity())
                        .reversed()
                        .thenComparing(item -> item.finding().ruleId())
                        .thenComparing(item -> item.finding().entry().filePath())
                        .thenComparingInt(item -> item.finding().entry().line()))
                .toList();

        report.append("\nFindings:\n");
        int visible = Math.min(MAX_FINDINGS, findings.size());
        for (int index = 0; index < visible; index++) {
            ChangedConfigRiskFinding item = findings.get(index);
            var finding = item.finding();
            var entry = finding.entry();
            report.append("- [").append(finding.severity()).append("] [")
                    .append(finding.ruleId()).append("] ")
                    .append(entry.key()).append(" (profile: ").append(entry.profile()).append(") at ")
                    .append(entry.filePath()).append(':').append(entry.line()).append('\n');
        }
        if (findings.size() > MAX_FINDINGS) {
            report.append("... and ").append(findings.size() - MAX_FINDINGS).append(" more findings.\n");
        }
        return report.toString().stripTrailing();
    }
}
