package io.github.tjdgus903.springconfigguard.settings;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBCheckBox;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
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
        JBCheckBox checkBox = (JBCheckBox) findButton(component, "Analyze selected Spring configuration changes before commit");
        assertNotNull(checkBox);
        assertTrue(checkBox.isSelected());
        checkBox.setSelected(false);
        assertTrue(configurable.isModified());
        configurable.apply();
        assertFalse(SpringConfigGuardSettings.getInstance(getProject()).isCommitWarningEnabled());
        assertFalse(configurable.isModified());
        configurable.disposeUIResources();
    }

    public void testResetRulesToDefaultsDoesNotChangeCommitWarning() {
        SpringConfigGuardSettings settings = SpringConfigGuardSettings.getInstance(getProject());
        settings.setCommitWarningEnabled(false);
        settings.setRuleEnabled("SCG001", false);
        SpringConfigGuardConfigurable configurable = new SpringConfigGuardConfigurable(getProject());
        JComponent component = configurable.createComponent();
        JBCheckBox commitCheck = (JBCheckBox) findButton(component, "Analyze selected Spring configuration changes before commit");
        JBCheckBox ruleCheck = (JBCheckBox) findButton(component, "SCG001 — Risky Hibernate ddl-auto in production");
        AbstractButton resetRules = findButton(component, "Reset rules to defaults");
        assertNotNull(commitCheck); assertNotNull(ruleCheck); assertNotNull(resetRules);
        assertFalse(commitCheck.isSelected()); assertFalse(ruleCheck.isSelected());
        resetRules.doClick();
        assertTrue(ruleCheck.isSelected()); assertFalse(commitCheck.isSelected()); assertTrue(configurable.isModified());
        configurable.apply();
        assertTrue(settings.isRuleEnabled("SCG001")); assertFalse(settings.isCommitWarningEnabled());
        configurable.disposeUIResources();
    }

    public void testBulkRuleControlsAreUiOnlyUntilApplyAndPreserveCommitWarning() {
        SpringConfigGuardSettings settings = SpringConfigGuardSettings.getInstance(getProject());
        settings.setCommitWarningEnabled(false);
        SpringConfigGuardConfigurable configurable = new SpringConfigGuardConfigurable(getProject());
        JComponent component = configurable.createComponent();
        JBCheckBox commitCheck = (JBCheckBox) findButton(component, "Analyze selected Spring configuration changes before commit");
        JBCheckBox ruleCheck = (JBCheckBox) findButton(component, "SCG001 — Risky Hibernate ddl-auto in production");
        AbstractButton disableAll = findButton(component, "Disable all rules");
        AbstractButton enableAll = findButton(component, "Enable all rules");
        assertNotNull(commitCheck); assertNotNull(ruleCheck); assertNotNull(disableAll); assertNotNull(enableAll);

        disableAll.doClick();
        assertFalse(ruleCheck.isSelected());
        assertFalse(commitCheck.isSelected());
        assertTrue(settings.isRuleEnabled("SCG001"));
        configurable.apply();
        assertFalse(settings.isRuleEnabled("SCG001"));
        assertFalse(settings.isCommitWarningEnabled());

        enableAll.doClick();
        assertTrue(ruleCheck.isSelected());
        assertFalse(commitCheck.isSelected());
        assertFalse(settings.isRuleEnabled("SCG001"));
        configurable.apply();
        assertTrue(settings.isRuleEnabled("SCG001"));
        assertFalse(settings.isCommitWarningEnabled());
        configurable.disposeUIResources();
    }

    public void testEnabledRuleSummaryTracksPersistedIndividualAndBulkSelections() {
        SpringConfigGuardSettings settings = SpringConfigGuardSettings.getInstance(getProject());
        settings.setRuleEnabled("SCG001", false);
        SpringConfigGuardConfigurable configurable = new SpringConfigGuardConfigurable(getProject());
        JComponent component = configurable.createComponent();
        JBCheckBox ruleCheck = (JBCheckBox) findButton(component, "SCG001 — Risky Hibernate ddl-auto in production");
        AbstractButton disableAll = findButton(component, "Disable all rules");
        AbstractButton enableAll = findButton(component, "Enable all rules");
        AbstractButton resetRules = findButton(component, "Reset rules to defaults");
        assertNotNull(ruleCheck); assertNotNull(disableAll); assertNotNull(enableAll); assertNotNull(resetRules);
        assertNotNull(findLabel(component, "Enabled rules: 12 of 13"));

        ruleCheck.doClick();
        assertNotNull(findLabel(component, "Enabled rules: 13 of 13"));
        assertFalse(settings.isRuleEnabled("SCG001"));

        disableAll.doClick();
        assertNotNull(findLabel(component, "Enabled rules: 0 of 13"));
        enableAll.doClick();
        assertNotNull(findLabel(component, "Enabled rules: 13 of 13"));
        resetRules.doClick();
        assertNotNull(findLabel(component, "Enabled rules: 13 of 13"));

        settings.setRuleEnabled("SCG002", false);
        configurable.reset();
        assertNotNull(findLabel(component, "Enabled rules: 11 of 13"));
        configurable.disposeUIResources();
    }

    public void testAllRulesDisabledWarningTracksUiSelectionAndPersistedState() {
        SpringConfigGuardSettings settings = SpringConfigGuardSettings.getInstance(getProject());
        settings.setCommitWarningEnabled(false);
        SpringConfigGuardConfigurable configurable = new SpringConfigGuardConfigurable(getProject());
        JComponent component = configurable.createComponent();
        AbstractButton disableAll = findButton(component, "Disable all rules");
        JLabel warning = findLabel(component, "All rules are disabled; SCG findings will not be reported.");
        assertNotNull(disableAll); assertNotNull(warning);
        assertFalse(warning.isVisible());

        disableAll.doClick();
        assertTrue(warning.isVisible());
        assertFalse(findButton(component, "Analyze selected Spring configuration changes before commit").isSelected());
        configurable.apply();
        configurable.disposeUIResources();

        SpringConfigGuardConfigurable reopened = new SpringConfigGuardConfigurable(getProject());
        JComponent reopenedComponent = reopened.createComponent();
        JLabel reopenedWarning = findLabel(reopenedComponent, "All rules are disabled; SCG findings will not be reported.");
        JBCheckBox oneRule = (JBCheckBox) findButton(reopenedComponent, "SCG001 — Risky Hibernate ddl-auto in production");
        AbstractButton resetRules = findButton(reopenedComponent, "Reset rules to defaults");
        assertNotNull(reopenedWarning); assertNotNull(oneRule); assertNotNull(resetRules);
        assertTrue(reopenedWarning.isVisible());

        oneRule.doClick();
        assertFalse(reopenedWarning.isVisible());
        assertFalse(settings.isRuleEnabled("SCG001"));
        resetRules.doClick();
        assertFalse(reopenedWarning.isVisible());
        assertFalse(settings.isCommitWarningEnabled());
        reopened.disposeUIResources();
    }

    private static JLabel findLabel(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) return label;
            if (component instanceof Container container) {
                JLabel found = findLabel(container, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static AbstractButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof AbstractButton button && text.equals(button.getText())) return button;
            if (component instanceof Container container) {
                AbstractButton found = findButton(container, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
