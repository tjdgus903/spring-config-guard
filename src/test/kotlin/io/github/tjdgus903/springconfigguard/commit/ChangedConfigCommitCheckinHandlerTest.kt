package io.github.tjdgus903.springconfigguard.commit

import com.intellij.openapi.vcs.checkin.CheckinHandler.ReturnResult.COMMIT
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.tjdgus903.springconfigguard.diff.ChangedConfigCommitPrecheckResult
import io.github.tjdgus903.springconfigguard.model.Severity
import io.github.tjdgus903.springconfigguard.settings.SpringConfigGuardSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

class ChangedConfigCommitCheckinHandlerTest : BasePlatformTestCase() {
    fun testProjectSettingControlsWhetherCommitCheckRuns() {
        val settings = SpringConfigGuardSettings.getInstance(project)
        val handler = ChangedConfigCommitCheckinHandler(project)

        assertTrue(handler.isEnabled())
        settings.isCommitWarningEnabled = false
        assertFalse(handler.isEnabled())

        settings.isCommitWarningEnabled = true
        assertTrue(handler.isEnabled())
    }

    fun testWarnsAndStillContinuesCommit() {
        var warnings = 0
        val handler = ChangedConfigCommitCheckinHandler(
            project,
            analysis = { ChangedConfigCommitPrecheckResult.reviewBeforeProceed(2, Severity.CRITICAL) },
            warningSink = { _, result ->
                assertEquals(2, result.findingCount())
                warnings++
            },
        )

        runBlocking {
            assertNull(handler.checkSelectedChanges(emptyList()))
        }

        assertEquals(1, warnings)
        assertEquals(COMMIT, handler.beforeCheckin())
    }

    fun testCleanAnalysisContinuesWithoutWarning() {
        var warnings = 0
        val handler = ChangedConfigCommitCheckinHandler(
            project,
            analysis = { ChangedConfigCommitPrecheckResult.proceed() },
            warningSink = { _, _ -> warnings++ },
        )

        runBlocking {
            assertNull(handler.checkSelectedChanges(emptyList()))
        }

        assertEquals(0, warnings)
        assertEquals(COMMIT, handler.beforeCheckin())
    }

    fun testAnalysisFailureContinuesWithoutWarning() {
        var warnings = 0
        val handler = ChangedConfigCommitCheckinHandler(
            project,
            analysis = { throw IllegalStateException("analysis failed") },
            warningSink = { _, _ -> warnings++ },
        )

        runBlocking {
            assertNull(handler.checkSelectedChanges(emptyList()))
        }

        assertEquals(0, warnings)
        assertEquals(COMMIT, handler.beforeCheckin())
    }

    fun testCancellationContinuesWithoutWarning() {
        val handler = ChangedConfigCommitCheckinHandler(
            project,
            analysis = { throw CancellationException("cancelled") },
            warningSink = { _, _ -> fail("Cancellation must not emit a warning") },
        )

        runBlocking {
            assertNull(handler.checkSelectedChanges(emptyList()))
        }
        assertEquals(COMMIT, handler.beforeCheckin())
    }
}
