package io.github.tjdgus903.springconfigguard.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Project-local preferences that never contain source or configuration data. */
@Service(Service.Level.PROJECT)
@State(name = "SpringConfigGuardSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class SpringConfigGuardSettings
        implements PersistentStateComponent<SpringConfigGuardSettings.SettingsState> {
    public static final Set<String> DEFAULT_PRODUCTION_ALIASES = Set.of("prod", "production", "prd");
    private SettingsState state = new SettingsState();

    public static SpringConfigGuardSettings getInstance(Project project) {
        return project.getService(SpringConfigGuardSettings.class);
    }

    @Override
    public @NotNull SettingsState getState() { return state; }

    @Override
    public void loadState(@NotNull SettingsState state) {
        if (state.disabledRuleIds == null) state.disabledRuleIds = new LinkedHashSet<>();
        if (state.productionAliases == null || state.productionAliases.isEmpty()) state.productionAliases = new LinkedHashSet<>(DEFAULT_PRODUCTION_ALIASES);
        else state.productionAliases = normalizeAliases(state.productionAliases);
        this.state = state;
    }

    public boolean isCommitWarningEnabled() { return state.commitWarningEnabled; }
    public void setCommitWarningEnabled(boolean enabled) { state.commitWarningEnabled = enabled; }

    public Set<String> getDisabledRuleIds() { return Set.copyOf(state.disabledRuleIds); }
    public Set<String> getProductionAliases() { return Set.copyOf(state.productionAliases); }
    public void setProductionAliases(Set<String> aliases) {
        Set<String> normalized = normalizeAliases(aliases);
        state.productionAliases = new LinkedHashSet<>(normalized.isEmpty() ? DEFAULT_PRODUCTION_ALIASES : normalized);
    }
    public void setProductionAliases(String aliases) {
        Set<String> parsedAliases = Arrays.stream(aliases == null ? new String[0] : aliases.split(","))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        setProductionAliases(parsedAliases);
    }
    private static LinkedHashSet<String> normalizeAliases(Iterable<String> aliases) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (aliases != null) for (String alias : aliases) {
            if (alias == null) continue;
            String value = alias.trim().toLowerCase(Locale.ROOT);
            if (!value.isBlank()) normalized.add(value);
        }
        return normalized;
    }
    public boolean isRuleEnabled(String ruleId) { return !state.disabledRuleIds.contains(ruleId); }
    public void setRuleEnabled(String ruleId, boolean enabled) {
        if (enabled) state.disabledRuleIds.remove(ruleId);
        else state.disabledRuleIds.add(ruleId);
    }

    public static final class SettingsState {
        public boolean commitWarningEnabled = true;
        public Set<String> disabledRuleIds = new LinkedHashSet<>();
        public Set<String> productionAliases = new LinkedHashSet<>(DEFAULT_PRODUCTION_ALIASES);
    }
}
