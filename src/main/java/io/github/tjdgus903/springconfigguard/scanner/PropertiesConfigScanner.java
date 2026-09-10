package io.github.tjdgus903.springconfigguard.scanner;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Parses Java .properties files while preserving logical-property start lines. */
public final class PropertiesConfigScanner {

    public List<ConfigEntry> scan(String content, String filePath, String profile) {
        List<ConfigEntry> entries = new ArrayList<>();
        String[] physicalLines = content.split("\\R", -1);

        StringBuilder logical = new StringBuilder();
        int logicalStartLine = 1;
        boolean collecting = false;

        for (int i = 0; i < physicalLines.length; i++) {
            String line = physicalLines[i];
            if (!collecting) {
                logicalStartLine = i + 1;
                collecting = true;
            }

            logical.append(line).append('\n');
            if (continues(line)) {
                continue;
            }

            parseLogicalProperty(logical.toString(), logicalStartLine, filePath, profile, entries);
            logical.setLength(0);
            collecting = false;
        }

        if (logical.length() > 0) {
            parseLogicalProperty(logical.toString(), logicalStartLine, filePath, profile, entries);
        }

        return List.copyOf(entries);
    }

    private void parseLogicalProperty(
            String text,
            int line,
            String filePath,
            String profile,
            List<ConfigEntry> entries
    ) {
        String trimmed = text.stripLeading();
        if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
            return;
        }

        Properties properties = new Properties();
        try {
            properties.load(new StringReader(text));
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to parse properties content", e);
        }

        for (Map.Entry<Object, Object> property : properties.entrySet()) {
            entries.add(new ConfigEntry(
                    String.valueOf(property.getKey()),
                    String.valueOf(property.getValue()),
                    profile,
                    filePath,
                    line
            ));
        }
    }

    private boolean continues(String line) {
        int slashCount = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            slashCount++;
        }
        return slashCount % 2 == 1;
    }
}
