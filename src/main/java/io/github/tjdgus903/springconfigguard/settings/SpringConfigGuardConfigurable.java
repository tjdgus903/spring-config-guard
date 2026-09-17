package io.github.tjdgus903.springconfigguard.settings;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import io.github.tjdgus903.springconfigguard.rule.ConfigRule;
import io.github.tjdgus903.springconfigguard.rule.MvpRuleRegistry;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SpringConfigGuardConfigurable implements SearchableConfigurable {
    private final Project project;
    private JBCheckBox commitWarningEnabled;
    private final Map<String, JBCheckBox> ruleChecks = new LinkedHashMap<>();

    public SpringConfigGuardConfigurable(Project project) { this.project = project; }

    @Override public @NotNull String getId() { return "io.github.tjdgus903.springconfigguard.settings"; }
    @Override public @Nls String getDisplayName() { return "Spring Config Guard"; }

    @Override
    public @Nullable JComponent createComponent() {
        JPanel rules = new JPanel(new GridLayout(0, 1, 0, 2));
        for (ConfigRule rule : MvpRuleRegistry.rules()) {
            JBCheckBox check = new JBCheckBox(rule.id() + " — " + readableName(rule.id()));
            ruleChecks.put(rule.id(), check);
            rules.add(check);
        }
        commitWarningEnabled = new JBCheckBox("Analyze selected Spring configuration changes before commit");
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(JBUI.Borders.empty(10));
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.add(commitWarningEnabled, BorderLayout.NORTH);
        content.add(rules, BorderLayout.CENTER);
        panel.add(content, BorderLayout.NORTH);
        reset();
        return panel;
    }

    private String readableName(String id) { return "Enable " + id + " checks"; }

    @Override public boolean isModified() {
        if (commitWarningEnabled != null && commitWarningEnabled.isSelected() != settings().isCommitWarningEnabled()) return true;
        return ruleChecks.entrySet().stream().anyMatch(e -> e.getValue().isSelected() != settings().isRuleEnabled(e.getKey()));
    }

    @Override public void apply() {
        if (commitWarningEnabled != null) settings().setCommitWarningEnabled(commitWarningEnabled.isSelected());
        ruleChecks.forEach((id, check) -> settings().setRuleEnabled(id, check.isSelected()));
    }

    @Override public void reset() {
        if (commitWarningEnabled != null) commitWarningEnabled.setSelected(settings().isCommitWarningEnabled());
        ruleChecks.forEach((id, check) -> check.setSelected(settings().isRuleEnabled(id)));
    }

    @Override public void disposeUIResources() {
        commitWarningEnabled = null; ruleChecks.clear();
    }

    private SpringConfigGuardSettings settings() { return SpringConfigGuardSettings.getInstance(project); }
}
