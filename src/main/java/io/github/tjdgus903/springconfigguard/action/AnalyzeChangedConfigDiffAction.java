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
import io.github.tjdgus903.springconfigguard.diff.ConfigDiffAnalysis;
import io.github.tjdgus903.springconfigguard.diff.ConfigEntryDiffAnalyzer;
import io.github.tjdgus903.springconfigguard.diff.ChangedConfigRiskAnalysis;
import io.github.tjdgus903.springconfigguard.diff.ChangedConfigRiskAnalyzer;
import io.github.tjdgus903.springconfigguard.diff.ChangedConfigRiskReportFormatter;
import io.github.tjdgus903.springconfigguard.project.ChangedConfigEntries;
import io.github.tjdgus903.springconfigguard.project.VcsChangedConfigCollector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.CancellablePromise;

import java.util.concurrent.CancellationException;

/** Collects local changed Spring configuration revisions and sends them to the diff core. */
public final class AnalyzeChangedConfigDiffAction extends AnAction {
    private final VcsChangedConfigCollector collector = new VcsChangedConfigCollector();
    private final ConfigEntryDiffAnalyzer analyzer = new ConfigEntryDiffAnalyzer();
    private final ChangedConfigRiskAnalyzer riskAnalyzer = new ChangedConfigRiskAnalyzer();
    private final ChangedConfigRiskReportFormatter reportFormatter = new ChangedConfigRiskReportFormatter();

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        ModalityState modality = ModalityState.defaultModalityState();
        CancellablePromise<String> task = ReadAction.nonBlocking(() -> {
                    ChangedConfigEntries entries = collector.collect(project);
                    ConfigDiffAnalysis diff = analyzer.analyze(entries.before(), entries.after());
                    ChangedConfigRiskAnalysis risk = riskAnalyzer.analyze(diff);
                    return reportFormatter.format(diff, risk);
                })
                .expireWith(project)
                .coalesceBy(AnalyzeChangedConfigDiffAction.class, project)
                .finishOnUiThread(modality, report -> new ChangedConfigReportDialog(project, report).show())
                .submit(AppExecutorUtil.getAppExecutorService());
        task.onError(error -> {
            if (task.isCancelled() || error instanceof ProcessCanceledException || error instanceof CancellationException) {
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed() && !task.isCancelled()) {
                    Messages.showErrorDialog(project,
                            "Could not analyze changed configuration. Try again.",
                            "Spring Config Guard - Changed Configuration");
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
