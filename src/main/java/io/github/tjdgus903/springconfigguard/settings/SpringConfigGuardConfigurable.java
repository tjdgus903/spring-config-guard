package io.github.tjdgus903.springconfigguard.settings;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Settings UI for the warning-only local commit check. */
public final class SpringConfigGuardConfigurable implements SearchableConfigurable {
    private final Project project;
    private JBCheckBox commitWarningEnabled;

    public SpringConfigGuardConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public @NotNull String getId() {
        return "io.github.tjdgus903.springconfigguard.settings";
    }

    @Override
    public @Nls String getDisplayName() {
        return "Spring Config Guard";
    }

    @Override
    public @Nullable JComponent createComponent() {
        commitWarningEnabled = new JBCheckBox(
                "Analyze selected Spring configuration changes before commit"
        );
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        panel.add(commitWarningEnabled, BorderLayout.NORTH);
        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        return commitWarningEnabled != null
                && commitWarningEnabled.isSelected() != settings().isCommitWarningEnabled();
    }

    @Override
    public void apply() {
        if (commitWarningEnabled != null) {
            settings().setCommitWarningEnabled(commitWarningEnabled.isSelected());
        }
    }

    @Override
    public void reset() {
        if (commitWarningEnabled != null) {
            commitWarningEnabled.setSelected(settings().isCommitWarningEnabled());
        }
    }

    @Override
    public void disposeUIResources() {
        commitWarningEnabled = null;
    }

    private SpringConfigGuardSettings settings() {
        return SpringConfigGuardSettings.getInstance(project);
    }
}
