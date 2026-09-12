package io.github.tjdgus903.springconfigguard.settings;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBCheckBox;

import javax.swing.JPanel;

public final class SpringConfigGuardSettingsTest extends BasePlatformTestCase {
    public void testCommitWarningIsEnabledByDefault() {
        assertTrue(SpringConfigGuardSettings.getInstance(getProject()).isCommitWarningEnabled());
    }

    public void testCommitWarningStateCanBeDisabledAndRestored() {
        SpringConfigGuardSettings settings = SpringConfigGuardSettings.getInstance(getProject());

        settings.setCommitWarningEnabled(false);
        assertFalse(settings.isCommitWarningEnabled());

        SpringConfigGuardSettings.SettingsState restored = new SpringConfigGuardSettings.SettingsState();
        restored.commitWarningEnabled = true;
        settings.loadState(restored);
        assertTrue(settings.isCommitWarningEnabled());
    }

    public void testConfigurableAppliesTheProjectPreference() {
        SpringConfigGuardConfigurable configurable = new SpringConfigGuardConfigurable(getProject());
        JPanel panel = (JPanel) configurable.createComponent();
        JBCheckBox checkBox = (JBCheckBox) panel.getComponent(0);

        assertTrue(checkBox.isSelected());
        checkBox.setSelected(false);
        assertTrue(configurable.isModified());

        configurable.apply();
        assertFalse(SpringConfigGuardSettings.getInstance(getProject()).isCommitWarningEnabled());
        assertFalse(configurable.isModified());
        configurable.disposeUIResources();
    }
}
