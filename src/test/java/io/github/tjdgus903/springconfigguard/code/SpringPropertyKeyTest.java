package io.github.tjdgus903.springconfigguard.code;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpringPropertyKeyTest {

    @Test
    void convertsCamelCaseToKebabCase() {
        assertEquals("payment-api-url", SpringPropertyKey.toKebabCase("paymentApiUrl"));
        assertEquals("max-retry-count", SpringPropertyKey.toKebabCase("maxRetryCount"));
    }

    @Test
    void preservesCommonAcronymBoundaries() {
        assertEquals("url", SpringPropertyKey.toKebabCase("URL"));
        assertEquals("url-value", SpringPropertyKey.toKebabCase("URLValue"));
        assertEquals("ssl-config", SpringPropertyKey.toKebabCase("SSLConfig"));
    }

    @Test
    void handlesDigitsAndExistingSeparators() {
        assertEquals("http2-enabled", SpringPropertyKey.toKebabCase("http2Enabled"));
        assertEquals("retry-count", SpringPropertyKey.toKebabCase("retry_count"));
        assertEquals("already-kebab", SpringPropertyKey.toKebabCase("already-kebab"));
    }

    @Test
    void joinsPrefixAndFieldKey() {
        assertEquals("payment.api.url", SpringPropertyKey.withPrefix("payment.api", "url"));
        assertEquals("payment.api.max-retry-count", SpringPropertyKey.withPrefix("payment.api.", "maxRetryCount"));
        assertEquals("timeout", SpringPropertyKey.withPrefix("", "timeout"));
        assertEquals("timeout", SpringPropertyKey.withPrefix(null, "timeout"));
    }

    @Test
    void rejectsBlankOrNonIdentifierInput() {
        assertThrows(NullPointerException.class, () -> SpringPropertyKey.toKebabCase(null));
        assertThrows(IllegalArgumentException.class, () -> SpringPropertyKey.toKebabCase("   "));
        assertThrows(IllegalArgumentException.class, () -> SpringPropertyKey.toKebabCase("$$$"));
    }
}
