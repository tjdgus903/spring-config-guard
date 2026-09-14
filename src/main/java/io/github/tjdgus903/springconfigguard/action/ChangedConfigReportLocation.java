package io.github.tjdgus903.springconfigguard.action;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts value-free source locations from rendered changed-configuration finding lines. */
record ChangedConfigReportLocation(String path, int line) {
    private static final Pattern FINDING_LOCATION = Pattern.compile("^- \\[.+] \\[SCG[^]]+] .+ at (.+):(\\d+)$");

    static Optional<ChangedConfigReportLocation> parse(String reportLine) {
        if (reportLine == null) {
            return Optional.empty();
        }
        Matcher matcher = FINDING_LOCATION.matcher(reportLine);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            int line = Integer.parseInt(matcher.group(2));
            return line > 0 ? Optional.of(new ChangedConfigReportLocation(matcher.group(1), line))
                    : Optional.empty();
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
