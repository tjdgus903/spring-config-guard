package io.github.tjdgus903.springconfigguard.rule;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;

import java.util.Optional;

public interface ConfigRule {
    String id();
    Optional<Finding> check(ConfigEntry entry, ConfigContext context);
}
