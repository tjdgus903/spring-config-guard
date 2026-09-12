package io.github.tjdgus903.springconfigguard.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import io.github.tjdgus903.springconfigguard.diff.ConfigDiffAnalysis;
import io.github.tjdgus903.springconfigguard.diff.ConfigEntryDiffAnalyzer;
import io.github.tjdgus903.springconfigguard.project.ChangedConfigEntries;
import io.github.tjdgus903.springconfigguard.project.VcsChangedConfigCollector;
import org.jetbrains.annotations.NotNull;

/** Collects local changed Spring configuration revisions and sends them to the diff core. */
public final class AnalyzeChangedConfigDiffAction extends AnAction {
    private final VcsChangedConfigCollector collector = new VcsChangedConfigCollector();
    private final ConfigEntryDiffAnalyzer analyzer = new ConfigEntryDiffAnalyzer();

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        ConfigDiffAnalysis analysis = ReadAction.compute(() -> {
            ChangedConfigEntries entries = collector.collect(project);
            return analyzer.analyze(entries.before(), entries.after());
        });
        Messages.showInfoMessage(
                project,
                "Local changed Spring configuration: " + analysis.additions().size() + " added, "
                        + analysis.modifications().size() + " modified, "
                        + analysis.removals().size() + " removed.",
                "Spring Config Guard - Changed Configuration"
        );
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
