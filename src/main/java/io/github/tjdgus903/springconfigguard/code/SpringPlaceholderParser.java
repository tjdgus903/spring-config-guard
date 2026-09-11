package io.github.tjdgus903.springconfigguard.code;

import java.util.ArrayList;
import java.util.List;

/** Deterministically parses Spring-style ${key} and ${key:default} placeholders. */
public final class SpringPlaceholderParser {
    private SpringPlaceholderParser() {
    }

    public static List<PlaceholderReference> parse(String expression) {
        List<PlaceholderReference> result = new ArrayList<>();
        if (expression == null || expression.isEmpty()) {
            return result;
        }

        for (int i = 0; i < expression.length() - 1; i++) {
            if (expression.charAt(i) != '$' || expression.charAt(i + 1) != '{') {
                continue;
            }
            if (i > 0 && expression.charAt(i - 1) == '\\') {
                continue;
            }

            int end = findClosingBrace(expression, i + 2);
            if (end < 0) {
                break;
            }

            String body = expression.substring(i + 2, end);
            int separator = findDefaultSeparator(body);
            String key = (separator >= 0 ? body.substring(0, separator) : body).trim();
            String defaultValue = separator >= 0 ? body.substring(separator + 1) : null;
            if (!key.isEmpty()) {
                result.add(new PlaceholderReference(key, defaultValue, i, end + 1));
            }
            i = end;
        }
        return result;
    }

    private static int findClosingBrace(String expression, int contentStart) {
        int nested = 0;
        for (int i = contentStart; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '{') {
                nested++;
            } else if (ch == '}') {
                if (nested == 0) {
                    return i;
                }
                nested--;
            }
        }
        return -1;
    }

    private static int findDefaultSeparator(String body) {
        int nested = 0;
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == '{') {
                nested++;
            } else if (ch == '}') {
                nested = Math.max(0, nested - 1);
            } else if (ch == ':' && nested == 0) {
                return i;
            }
        }
        return -1;
    }

    public record PlaceholderReference(
            String key,
            String defaultValue,
            int startOffset,
            int endOffset
    ) {
    }
}
