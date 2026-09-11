package io.github.tjdgus903.springconfigguard.action;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextArea;

/** Scrollable, copyable, read-only report that does not block editing in the project. */
final class ConfigKeyMappingReportDialog extends DialogWrapper {
    private final JTextArea textArea;

    ConfigKeyMappingReportDialog(Project project, String report) {
        super(project, false);
        textArea = new JTextArea(report, 26, 100);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        setTitle("Spring Config Guard - Key Mapping");
        setModal(false);
        setCancelButtonText("Close");
        init();
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        return new JBScrollPane(textArea);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return textArea;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getCancelAction()};
    }
}
