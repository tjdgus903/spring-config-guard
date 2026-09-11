package io.github.tjdgus903.springconfigguard.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.concurrency.AppExecutorUtil;
import io.github.tjdgus903.springconfigguard.mapping.ConfigKeyMappingReportFormatter;
import io.github.tjdgus903.springconfigguard.project.ProjectConfigKeyMappingAnalyzer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.CancellablePromise;

import java.util.concurrent.CancellationException;

/** Runs project-wide key mapping without traversing project files on the UI thread. */
public final class AnalyzeConfigKeyMappingsAction extends AnAction {
    private final ProjectConfigKeyMappingAnalyzer analyzer = new ProjectConfigKeyMappingAnalyzer();
    private final ConfigKeyMappingReportFormatter formatter = new ConfigKeyMappingReportFormatter();

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        ModalityState modality = ModalityState.defaultModalityState();
        CancellablePromise<String> task = ReadAction.nonBlocking(() -> formatter.format(analyzer.analyze(project)))
                .inSmartMode(project)
                .withDocumentsCommitted(project)
                .expireWith(project)
                .coalesceBy(AnalyzeConfigKeyMappingsAction.class, project)
                .finishOnUiThread(modality, report -> new ConfigKeyMappingReportDialog(project, report).show())
                .submit(AppExecutorUtil.getAppExecutorService());
        task.onError(error -> {
            if (task.isCancelled() || error instanceof ProcessCanceledException || error instanceof CancellationException) {
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed() && !task.isCancelled()) {
                    Messages.showErrorDialog(project,
                            "Could not complete key mapping analysis. Try again.",
                            "Spring Config Guard - Key Mapping");
                }
            }, modality);
        });
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        event.getPresentation().setEnabledAndVisible(project != null && !project.isDisposed());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
