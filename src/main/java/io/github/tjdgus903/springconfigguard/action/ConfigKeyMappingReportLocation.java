package io.github.tjdgus903.springconfigguard.action;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts value-free source locations from rendered config-key mapping report lines. */
record ConfigKeyMappingReportLocation(String path, int line) {
    private static final Pattern LOCATION = Pattern.compile("^(.+):(\\d+)(?: \\(default present\\)| :: .+)?$");

    static Optional<ConfigKeyMappingReportLocation> parse(String reportLine) {
        if (reportLine == null) {
            return Optional.empty();
        }

        String locationText;
        if (reportLine.startsWith("  config [")) {
            int profileEnd = reportLine.indexOf("] ");
            if (profileEnd < 0) {
                return Optional.empty();
            }
            locationText = reportLine.substring(profileEnd + 2);
        } else if (reportLine.startsWith("  @Value ")) {
            locationText = reportLine.substring("  @Value ".length());
        } else if (reportLine.startsWith("  @ConfigurationProperties ")) {
            locationText = reportLine.substring("  @ConfigurationProperties ".length());
        } else {
            return Optional.empty();
        }

        Matcher matcher = LOCATION.matcher(locationText);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            int line = Integer.parseInt(matcher.group(2));
            String path = matcher.group(1);
            return line > 0 && !path.isBlank()
                    ? Optional.of(new ConfigKeyMappingReportLocation(path, line))
                    : Optional.empty();
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
