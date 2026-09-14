package io.github.tjdgus903.springconfigguard.action;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Nonmodal, scrollable, copyable report for local changed-configuration metadata. */
final class ChangedConfigReportDialog extends DialogWrapper {
    private final JTextArea textArea;

    ChangedConfigReportDialog(Project project, String report) {
        super(project, false);
        textArea = new JTextArea(report, 24, 100);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        textArea.setToolTipText("Double-click a finding to open its source line");
        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    openFindingAt(project, event);
                }
            }
        });
        setTitle("Spring Config Guard - Changed Configuration");
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

    private void openFindingAt(Project project, MouseEvent event) {
        int offset = textArea.viewToModel2D(event.getPoint());
        if (offset < 0) {
            return;
        }
        try {
            int lineIndex = textArea.getLineOfOffset(offset);
            int start = textArea.getLineStartOffset(lineIndex);
            int end = textArea.getLineEndOffset(lineIndex);
            String reportLine = textArea.getText(start, end - start).stripTrailing();
            ChangedConfigReportLocation.parse(reportLine).ifPresent(location -> navigate(project, location));
        } catch (BadLocationException ignored) {
            // The immutable report changed between coordinate and document lookup; ignore the click.
        }
    }

    private void navigate(Project project, ChangedConfigReportLocation location) {
        String path = location.path();
        if (!FileUtil.isAbsolute(path)) {
            String basePath = project.getBasePath();
            if (basePath == null) {
                return;
            }
            path = basePath + "/" + path;
        }
        VirtualFile file = LocalFileSystem.getInstance()
                .findFileByPath(FileUtil.toSystemIndependentName(path));
        VirtualFile baseDirectory = project.getBaseDir();
        if (file == null || file.isDirectory() || baseDirectory == null
                || !VfsUtilCore.isAncestor(baseDirectory, file, false)) {
            return;
        }
        new OpenFileDescriptor(project, file, location.line() - 1, 0).navigate(true);
    }
}
