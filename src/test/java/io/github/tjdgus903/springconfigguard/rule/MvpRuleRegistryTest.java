package io.github.tjdgus903.springconfigguard.rule;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MvpRuleRegistryTest {

    @Test
    void exposesAllMvpRulesInStableOrderWithoutDuplicates() {
        List<String> ids = MvpRuleRegistry.rules().stream()
                .map(ConfigRule::id)
                .toList();

        assertEquals(List.of("SCG001", "SCG002", "SCG003", "SCG004", "SCG005"), ids);
        Set<String> unique = ids.stream().collect(Collectors.toSet());
        assertEquals(ids.size(), unique.size());
    }
}
