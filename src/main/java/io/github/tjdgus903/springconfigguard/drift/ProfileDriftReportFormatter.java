package io.github.tjdgus903.springconfigguard.drift;

import java.util.Comparator;
import java.util.List;

/** Formats local profile drift results for a compact IDE report. */
public final class ProfileDriftReportFormatter {
    private static final int MAX_FINDINGS = 20;

    public String format(ProfileDriftAnalysis analysis) {
        if (analysis.effectiveValues().isEmpty()) {
            return "No Spring Boot application configuration files were found in project content.";
        }

        long riskCount = analysis.findings().stream()
                .filter(finding -> finding.kind() == ProfileDriftKind.RISK)
                .count();
        long differenceCount = analysis.findings().stream()
                .filter(finding -> finding.kind() == ProfileDriftKind.DIFFERENCE)
                .count();

        StringBuilder report = new StringBuilder();
        report.append("Profile Drift Analysis\n\n")
                .append("Risk findings: ").append(riskCount).append('\n')
                .append("Profile differences: ").append(differenceCount).append('\n');

        if (analysis.findings().isEmpty()) {
            return report.append("\nNo drift findings detected.").toString();
        }

        List<ProfileDriftFinding> ordered = analysis.findings().stream()
                .sorted(Comparator
                        .comparing((ProfileDriftFinding finding) -> finding.kind() == ProfileDriftKind.RISK ? 0 : 1)
                        .thenComparing(ProfileDriftFinding::profile)
                        .thenComparing(ProfileDriftFinding::key))
                .toList();

        report.append("\nFindings:\n");
        int visible = Math.min(MAX_FINDINGS, ordered.size());
        for (int i = 0; i < visible; i++) {
            ProfileDriftFinding finding = ordered.get(i);
            report.append("- [").append(finding.severity()).append("] ");
            if (finding.ruleId() != null) {
                report.append('[').append(finding.ruleId()).append("] ");
            }
            report.append(finding.profile())
                    .append(" :: ")
                    .append(finding.key())
                    .append(" — ")
                    .append(finding.message())
                    .append('\n');
        }

        if (ordered.size() > MAX_FINDINGS) {
            report.append("... and ")
                    .append(ordered.size() - MAX_FINDINGS)
                    .append(" more findings.");
        }

        return report.toString().stripTrailing();
    }
}
