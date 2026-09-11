package io.github.tjdgus903.springconfigguard.code;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpringPlaceholderParserTest {
    @Test
    void parsesKeyWithoutDefault() {
        var refs = SpringPlaceholderParser.parse("${payment.api.url}");
        assertEquals(1, refs.size());
        assertEquals("payment.api.url", refs.getFirst().key());
        assertNull(refs.getFirst().defaultValue());
    }

    @Test
    void parsesKeyWithDefaultContainingColon() {
        var refs = SpringPlaceholderParser.parse("${payment.api.url:http://localhost:8080}");
        assertEquals(1, refs.size());
        assertEquals("payment.api.url", refs.getFirst().key());
        assertEquals("http://localhost:8080", refs.getFirst().defaultValue());
    }

    @Test
    void parsesMultiplePlaceholders() {
        List<SpringPlaceholderParser.PlaceholderReference> refs =
                SpringPlaceholderParser.parse("${host}:${port:8080}");
        assertEquals(List.of("host", "port"), refs.stream().map(SpringPlaceholderParser.PlaceholderReference::key).toList());
        assertEquals("8080", refs.get(1).defaultValue());
    }

    @Test
    void ignoresEscapedAndMalformedPlaceholders() {
        assertEquals(0, SpringPlaceholderParser.parse("\\${ignored}").size());
        assertEquals(0, SpringPlaceholderParser.parse("${missing").size());
    }

    @Test
    void ignoresSpelOnlyExpression() {
        assertEquals(0, SpringPlaceholderParser.parse("#{systemProperties['user.home']}").size());
    }
}
