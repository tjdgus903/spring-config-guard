package io.github.tjdgus903.springconfigguard.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import io.github.tjdgus903.springconfigguard.drift.ProfileDriftAnalysis;
import io.github.tjdgus903.springconfigguard.drift.ProfileDriftAnalyzer;
import io.github.tjdgus903.springconfigguard.drift.ProfileDriftReportFormatter;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.project.ProjectConfigCollector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Runs deterministic profile drift analysis against local project configuration. */
public final class AnalyzeProfileDriftAction extends AnAction {
    private final ProjectConfigCollector collector = new ProjectConfigCollector();
    private final ProfileDriftAnalyzer analyzer = new ProfileDriftAnalyzer();
    private final ProfileDriftReportFormatter formatter = new ProfileDriftReportFormatter();

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }

        ProfileDriftAnalysis analysis = ReadAction.compute(() -> {
            List<ConfigEntry> entries = collector.collect(project);
            return analyzer.analyze(entries);
        });

        Messages.showInfoMessage(
                project,
                formatter.format(analysis),
                "Spring Config Guard - Profile Drift"
        );
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(event.getProject() != null);
    }
}
