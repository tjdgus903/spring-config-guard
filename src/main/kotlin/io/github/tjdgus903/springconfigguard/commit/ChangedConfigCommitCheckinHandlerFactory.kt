package io.github.tjdgus903.springconfigguard.commit

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.openapi.vcs.checkin.CommitCheck
import com.intellij.openapi.vcs.checkin.CommitInfo
import com.intellij.openapi.vcs.checkin.CommitProblem
import io.github.tjdgus903.springconfigguard.diff.ChangedConfigCommitPrecheck
import io.github.tjdgus903.springconfigguard.diff.ChangedConfigCommitPrecheckResult
import io.github.tjdgus903.springconfigguard.diff.ChangedConfigCommitPrecheckResult.Recommendation.REVIEW_BEFORE_PROCEED
import io.github.tjdgus903.springconfigguard.diff.ChangedConfigRiskAnalyzer
import io.github.tjdgus903.springconfigguard.diff.ConfigEntryDiffAnalyzer
import io.github.tjdgus903.springconfigguard.project.VcsChangedConfigCollector
import io.github.tjdgus903.springconfigguard.settings.SpringConfigGuardConfigurable
import io.github.tjdgus903.springconfigguard.settings.SpringConfigGuardSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Registers a warning-only local commit check for selected Spring configuration changes. */
class ChangedConfigCommitCheckinHandlerFactory : CheckinHandlerFactory() {
    override fun createHandler(
        panel: CheckinProjectPanel,
        commitContext: CommitContext,
    ): CheckinHandler = ChangedConfigCommitCheckinHandler(panel.project)
}

internal class ChangedConfigCommitCheckinHandler(
    private val project: Project,
    private val analysis: suspend (List<Change>) -> ChangedConfigCommitPrecheckResult = { changes ->
        analyzeSelectedChanges(project, changes)
    },
    private val warningSink: (Project, ChangedConfigCommitPrecheckResult) -> Unit = ::showWarning,
) : CheckinHandler(), CommitCheck, DumbAware {

    override fun getExecutionOrder(): CommitCheck.ExecutionOrder = CommitCheck.ExecutionOrder.EARLY

    override fun isEnabled(): Boolean =
        !project.isDisposed && SpringConfigGuardSettings.getInstance(project).isCommitWarningEnabled

    override suspend fun runCheck(commitInfo: CommitInfo): CommitProblem? =
        checkSelectedChanges(commitInfo.committedChanges)

    internal suspend fun checkSelectedChanges(changes: List<Change>): CommitProblem? {
        if (project.isDisposed) {
            return null
        }
        val result = try {
            analysis(changes.toList())
        } catch (_: CancellationException) {
            return null
        } catch (_: RuntimeException) {
            return null
        }

        if (result.recommendation() == REVIEW_BEFORE_PROCEED) {
            warningSink(project, result)
        }
        return null
    }

    /** Legacy commit mode fallback. This handler never cancels or closes a commit. */
    override fun beforeCheckin(): ReturnResult = ReturnResult.COMMIT
}

private suspend fun analyzeSelectedChanges(
    project: Project,
    changes: List<Change>,
): ChangedConfigCommitPrecheckResult = withContext(Dispatchers.IO) {
    ReadAction.compute<ChangedConfigCommitPrecheckResult, RuntimeException> {
        val entries = VcsChangedConfigCollector().collect(project, changes)
        val diff = ConfigEntryDiffAnalyzer().analyze(entries.before(), entries.after())
        val risks = ChangedConfigRiskAnalyzer().analyze(diff)
        ChangedConfigCommitPrecheck().evaluate(risks)
    }
}

private fun showWarning(project: Project, result: ChangedConfigCommitPrecheckResult) {
    val highestSeverity = result.highestSeverity().orElseThrow()
    NotificationGroupManager.getInstance()
        .getNotificationGroup("Spring Config Guard")
        .createNotification(
            "Risky Spring configuration changes detected",
            "${result.findingCount()} deterministic finding(s); highest severity: $highestSeverity. " +
                "The commit will continue.",
            NotificationType.WARNING,
        )
        .addAction(
            NotificationAction.createSimpleExpiring("Configure…") {
                ShowSettingsUtil.getInstance()
                    .showSettingsDialog(project, SpringConfigGuardConfigurable::class.java)
            },
        )
        .notify(project)
}
