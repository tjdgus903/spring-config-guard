package io.github.tjdgus903.springconfigguard.settings;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBCheckBox;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;

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
        JComponent component = configurable.createComponent();
        assertNotNull(component);
        JBCheckBox checkBox = findCheckBox(component, "Analyze selected Spring configuration changes before commit");
        assertNotNull(checkBox);

        assertTrue(checkBox.isSelected());
        checkBox.setSelected(false);
        assertTrue(configurable.isModified());

        configurable.apply();
        assertFalse(SpringConfigGuardSettings.getInstance(getProject()).isCommitWarningEnabled());
        assertFalse(configurable.isModified());
        configurable.disposeUIResources();
    }

    private static JBCheckBox findCheckBox(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JBCheckBox checkBox && text.equals(checkBox.getText())) {
                return checkBox;
            }
            if (component instanceof Container container) {
                JBCheckBox found = findCheckBox(container, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
