package io.github.tjdgus903.springconfigguard.settings;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import io.github.tjdgus903.springconfigguard.rule.ConfigRule;
import io.github.tjdgus903.springconfigguard.rule.MvpRuleRegistry;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SpringConfigGuardConfigurable implements SearchableConfigurable {
    private final Project project;
    private JBCheckBox commitWarningEnabled;
    private JBLabel ruleSelectionSummary;
    private JBLabel allRulesDisabledWarning;
    private JBTextField productionAliases;
    private final Map<String, JBCheckBox> ruleChecks = new LinkedHashMap<>();

    public SpringConfigGuardConfigurable(Project project) { this.project = project; }

    @Override public @NotNull String getId() { return "io.github.tjdgus903.springconfigguard.settings"; }
    @Override public @Nls String getDisplayName() { return "Spring Config Guard"; }

    @Override
    public @Nullable JComponent createComponent() {
        JPanel rules = new JPanel(new GridLayout(0, 1, 0, 2));
        for (ConfigRule rule : MvpRuleRegistry.rules()) {
            JBCheckBox check = new JBCheckBox(rule.id() + " — " + readableName(rule.id()));
            check.addActionListener(event -> updateRuleSelectionSummary());
            ruleChecks.put(rule.id(), check);
            rules.add(check);
        }

        JButton enableAllRules = new JButton("Enable all rules");
        enableAllRules.addActionListener(event -> setAllRuleChecks(true));
        JButton disableAllRules = new JButton("Disable all rules");
        disableAllRules.addActionListener(event -> setAllRuleChecks(false));
        JButton resetRules = new JButton("Reset rules to defaults");
        resetRules.addActionListener(event -> setAllRuleChecks(true));

        JPanel ruleActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        ruleActions.add(enableAllRules);
        ruleActions.add(disableAllRules);
        ruleActions.add(resetRules);

        ruleSelectionSummary = new JBLabel();
        allRulesDisabledWarning = new JBLabel("All rules are disabled; SCG findings will not be reported.");
        commitWarningEnabled = new JBCheckBox("Analyze selected Spring configuration changes before commit");
        productionAliases = new JBTextField();
        JPanel profileAliases = new JPanel(new BorderLayout(6, 0));
        profileAliases.add(new JBLabel("Production profile aliases (comma-separated; defaults: prod, production, prd):"), BorderLayout.WEST);
        profileAliases.add(productionAliases, BorderLayout.CENTER);
        JButton resetProductionAliases = new JButton("Reset production aliases");
        resetProductionAliases.addActionListener(event -> productionAliases.setText("prod, production, prd"));
        profileAliases.add(resetProductionAliases, BorderLayout.EAST);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(JBUI.Borders.empty(10));
        JPanel content = new JPanel(new BorderLayout(0, 8));
        JPanel ruleSelection = new JPanel(new BorderLayout(0, 4));
        JPanel ruleStatus = new JPanel(new GridLayout(0, 1, 0, 2));
        ruleStatus.add(ruleSelectionSummary);
        ruleStatus.add(allRulesDisabledWarning);
        ruleSelection.add(ruleStatus, BorderLayout.NORTH);
        ruleSelection.add(rules, BorderLayout.CENTER);
        JPanel preferences = new JPanel(new GridLayout(0, 1, 0, 4));
        preferences.add(commitWarningEnabled);
        preferences.add(profileAliases);
        content.add(preferences, BorderLayout.NORTH);
        content.add(ruleSelection, BorderLayout.CENTER);
        content.add(ruleActions, BorderLayout.SOUTH);
        panel.add(content, BorderLayout.NORTH);
        reset();
        return panel;
    }

    private void setAllRuleChecks(boolean selected) {
        ruleChecks.values().forEach(check -> check.setSelected(selected));
        updateRuleSelectionSummary();
    }

    private void updateRuleSelectionSummary() {
        if (ruleSelectionSummary == null) return;
        long enabled = ruleChecks.values().stream().filter(JBCheckBox::isSelected).count();
        ruleSelectionSummary.setText("Enabled rules: " + enabled + " of " + ruleChecks.size());
        if (allRulesDisabledWarning != null) allRulesDisabledWarning.setVisible(enabled == 0);
    }

    private String readableName(String id) {
        return switch (id) {
            case "SCG001" -> "Risky Hibernate ddl-auto in production";
            case "SCG002" -> "Actuator wildcard exposure";
            case "SCG003" -> "Stacktrace exposure";
            case "SCG004" -> "Root DEBUG logging";
            case "SCG005" -> "SQL logging enabled";
            case "SCG006" -> "Error-message exposure";
            case "SCG007" -> "Binding-error exposure";
            case "SCG008" -> "H2 console enabled in production";
            case "SCG009" -> "Unsanitized Actuator environment values";
            case "SCG010" -> "Unsanitized Actuator configuration-property values";
            case "SCG011" -> "Actuator health details exposed to every user";
            case "SCG012" -> "Actuator health components exposed to every user";
            case "SCG013" -> "Actuator shutdown endpoint enabled in production";
            default -> "Configuration risk check";
        };
    }

    @Override public boolean isModified() {
        if (commitWarningEnabled != null && commitWarningEnabled.isSelected() != settings().isCommitWarningEnabled()) return true;
        if (productionAliases != null && !normalizedAliasText(productionAliases.getText()).equals(normalizedAliasText(String.join(",", settings().getProductionAliases())))) return true;
        return ruleChecks.entrySet().stream().anyMatch(e -> e.getValue().isSelected() != settings().isRuleEnabled(e.getKey()));
    }

    @Override public void apply() {
        if (commitWarningEnabled != null) settings().setCommitWarningEnabled(commitWarningEnabled.isSelected());
        if (productionAliases != null) settings().setProductionAliases(productionAliases.getText());
        ruleChecks.forEach((id, check) -> settings().setRuleEnabled(id, check.isSelected()));
    }

    @Override public void reset() {
        if (commitWarningEnabled != null) commitWarningEnabled.setSelected(settings().isCommitWarningEnabled());
        if (productionAliases != null) productionAliases.setText(String.join(", ", settings().getProductionAliases()));
        ruleChecks.forEach((id, check) -> check.setSelected(settings().isRuleEnabled(id)));
        updateRuleSelectionSummary();
    }

    @Override public void disposeUIResources() {
        commitWarningEnabled = null;
        productionAliases = null;
        ruleSelectionSummary = null;
        allRulesDisabledWarning = null;
        ruleChecks.clear();
    }

    private static java.util.Set<String> normalizedAliasText(String value) {
        java.util.Set<String> aliases = new java.util.HashSet<>();
        if (value != null) for (String alias : value.split(",")) {
            String normalized = alias.trim().toLowerCase(java.util.Locale.ROOT);
            if (!normalized.isBlank()) aliases.add(normalized);
        }
        return aliases;
    }

    private SpringConfigGuardSettings settings() { return SpringConfigGuardSettings.getInstance(project); }
}
