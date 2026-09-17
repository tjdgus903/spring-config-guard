package io.github.tjdgus903.springconfigguard.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

/** Project-local preferences that never contain source or configuration data. */
@Service(Service.Level.PROJECT)
@State(name = "SpringConfigGuardSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class SpringConfigGuardSettings
        implements PersistentStateComponent<SpringConfigGuardSettings.SettingsState> {
    private SettingsState state = new SettingsState();

    public static SpringConfigGuardSettings getInstance(Project project) {
        return project.getService(SpringConfigGuardSettings.class);
    }

    @Override
    public @NotNull SettingsState getState() { return state; }

    @Override
    public void loadState(@NotNull SettingsState state) {
        if (state.disabledRuleIds == null) state.disabledRuleIds = new LinkedHashSet<>();
        this.state = state;
    }

    public boolean isCommitWarningEnabled() { return state.commitWarningEnabled; }
    public void setCommitWarningEnabled(boolean enabled) { state.commitWarningEnabled = enabled; }

    public Set<String> getDisabledRuleIds() { return Set.copyOf(state.disabledRuleIds); }
    public boolean isRuleEnabled(String ruleId) { return !state.disabledRuleIds.contains(ruleId); }
    public void setRuleEnabled(String ruleId, boolean enabled) {
        if (enabled) state.disabledRuleIds.remove(ruleId);
        else state.disabledRuleIds.add(ruleId);
    }

    public static final class SettingsState {
        public boolean commitWarningEnabled = true;
        public Set<String> disabledRuleIds = new LinkedHashSet<>();
    }
}
