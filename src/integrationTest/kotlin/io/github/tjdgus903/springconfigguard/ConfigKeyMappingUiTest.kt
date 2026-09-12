package io.github.tjdgus903.springconfigguard

import com.intellij.driver.client.Remote
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.openFile
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.button
import com.intellij.driver.sdk.ui.components.elements.dialog
import com.intellij.driver.sdk.ui.components.elements.textField
import com.intellij.driver.sdk.ui.present
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.ui.shouldNot
import com.intellij.driver.sdk.ui.ui
import com.intellij.driver.sdk.waitFor
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.NoCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.Starter
import com.intellij.tools.ide.performanceTesting.commands.SdkObject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.minutes

/** Exercises the installed ZIP and visible Swing report in a separate, real IDE process. */
class ConfigKeyMappingUiTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    private val artifacts = Path.of(System.getProperty("scg.ui.artifacts"))
    private val ideFailures = CopyOnWriteArrayList<String>()

    @Test
    fun installedPluginShowsReportAndReflectsAnEditorChange() {
        val originalDi = di
        di = DI {
            extend(originalDi)
            bindSingleton<CIServer>(overrides = true) {
                object : CIServer by NoCIServer {
                    override fun reportTestFailure(testName: String, message: String, details: String, linkToLogs: String?) {
                        ideFailures.add("$testName: $message\n$details")
                    }

                    override fun isTestFailureShouldBeIgnored(message: String) = false
                }
            }
        }

        try {
            val project = unpackSample()
            initializeGitBaseline(project)
            val context = Starter.newContext(
                "spring-config-guard-key-mapping",
                TestCase(IdeProductProvider.IU, LocalProjectInfo(project))
            ).apply {
                PluginConfigurator(this).installPluginFromPath(Path.of(System.getProperty("path.to.build.plugin")))
                applyVMOptionsPatch {
                    withXmx(2048)
                    addSystemProperty("idea.trust.all.projects", true)
                    addSystemProperty("ide.show.tips.on.startup.default.value", false)
                }
            }.setupSdk(SdkObject(
                sdkName = "21",
                sdkType = "JavaSDK",
                sdkPath = Path.of(System.getProperty("scg.jdk.home"))
            ))

            Files.createDirectories(artifacts)
            Files.writeString(artifacts.resolve("ide-test-home.txt"), context.paths.testHome.toString())
            context.runIdeWithDriver(runTimeout = 12.minutes).useDriverAndCloseIde {
                try {
                    waitForIndicators(5.minutes)
                    openFile("src/main/resources/application-prod.properties")
                    val frame = ui.ideFrame()
                    frame.toFront()
                    invokeAction(ACTION_ID, component = frame.component)

                    val reportDialog = ui.dialog(title = REPORT_TITLE).shouldBe(present)
                    val reportArea = reportDialog.textField { byJavaClass("javax.swing.JTextArea") }.shouldBe(present)
                    val initialReport = reportArea.text
                    assertReport(initialReport, matched = 3, unmatchedProperties = 1)
                    assertFalse(cast(reportDialog.component, AwtDialog::class).isModal(), "Report must allow project editing")
                    assertFalse(cast(reportArea.component, ReportTextComponent::class).isEditable(), "Report must be read-only")
                    Files.writeString(artifacts.resolve("before-report.txt"), initialReport)
                    screenshot("before-report.png")

                    reportDialog.toFront()
                    reportDialog.button("Close").click()
                    reportDialog.shouldNot(present)

                    frame.toFront()
                    invokeAction(CHANGED_CONFIG_ACTION_ID, component = frame.component)
                    val changedDialog = ui.dialog(title = CHANGED_CONFIG_REPORT_TITLE).shouldBe(present)
                    val changedReportArea = changedDialog.textField {
                        byJavaClass("javax.swing.JTextArea")
                    }.shouldBe(present)
                    val changedReport = changedReportArea.text
                    assertChangedConfigurationReport(changedReport)
                    assertFalse(cast(changedDialog.component, AwtDialog::class).isModal(),
                        "Changed-configuration report must allow project editing")
                    assertFalse(cast(changedReportArea.component, ReportTextComponent::class).isEditable(),
                        "Changed-configuration report must be read-only")
                    Files.writeString(artifacts.resolve("changed-configuration-report.txt"), changedReport)
                    screenshot("changed-configuration-report.png")
                    changedDialog.button("Close").click()
                    changedDialog.shouldNot(present)

                    // Exercise the registered CommitCheck through IntelliJ's real non-modal commit UI.
                    frame.toFront()
                    invokeAction(COMMIT_PROJECT_ACTION_ID, component = frame.component)
                    val commitMessage = frame.x { byAccessibleName("Commit Message") }.shouldBe(present)
                    commitMessage.click()
                    commitMessage.keyboard { typeText(COMMIT_MESSAGE) }
                    val commitActions = frame.x {
                        byJavaClass("com.intellij.vcs.commit.CommitActionsPanel")
                    }.shouldBe(present)
                    commitActions.x { byVisibleText("Commit") }.shouldBe(present).click()

                    val warningTitleLabel = frame.x {
                        and(
                            byJavaClass("javax.swing.JLabel"),
                            contains(byVisibleText("Risky Spring configuration changes detected")),
                        )
                    }.shouldBe(present)
                    val warningContentLabel = frame.x {
                        byAccessibleName("5 deterministic finding(s); highest severity: CRITICAL. The commit will continue.")
                    }.shouldBe(present)
                    val warningText = listOf(
                        cast(warningTitleLabel.component, AwtLabel::class).getText(),
                        cast(warningContentLabel.component, AwtTextComponent::class).getText(),
                    ).joinToString("\n").replace(Regex("\\s+"), " ").trim()
                    assertCommitWarning(warningText)
                    Files.writeString(artifacts.resolve("commit-warning.txt"), warningText)
                    screenshot("commit-warning.png")

                    val configureAction = frame.x { byVisibleText("Configure…") }.shouldBe(present)
                    configureAction.click()
                    val settingsDialog = ui.dialog(title = "Settings").shouldBe(present)
                    settingsDialog.x {
                        byVisibleText("Analyze selected Spring configuration changes before commit")
                    }.shouldBe(present)
                    screenshot("commit-warning-settings.png")
                    settingsDialog.button("Cancel").click()
                    settingsDialog.shouldNot(present)

                    waitFor("the warning-only commit to complete", timeout = 1.minutes) {
                        gitOutput(project, "log", "-1", "--pretty=%s").trim() == COMMIT_MESSAGE
                    }
                    assertTrue(
                        gitOutput(project, "status", "--porcelain", "--", CONFIG_PATH).isBlank(),
                        "The risky configuration must be committed after the warning",
                    )

                    // Mutate the open IDE document and verify a fresh mapping report reflects it.
                    frame.toFront()
                    val editor = frame.codeEditor().shouldBe(present)
                    assertTrue(editor.isEditable(), "Sample editor must remain editable")
                    editor.text = editor.text.trimEnd() + "\ndemo.region=UI_SAMPLE_REGION\n"
                    assertTrue(editor.text.contains("demo.region=UI_SAMPLE_REGION"))

                    frame.toFront()
                    invokeAction(ACTION_ID, component = frame.component)
                    val updatedDialog = ui.dialog(title = REPORT_TITLE).shouldBe(present)
                    val updatedReport = updatedDialog.textField { byJavaClass("javax.swing.JTextArea") }.shouldBe(present).text
                    assertReport(updatedReport, matched = 4, unmatchedProperties = 0)
                    assertTrue(updatedReport.substringBefore("\nConfig entries without a matching Java reference:\n")
                        .contains("- demo.region\n"), "New config key must move into the matched section")
                    Files.writeString(artifacts.resolve("after-report.txt"), updatedReport)
                    screenshot("after-report.png")
                    updatedDialog.button("Close").click()
                    updatedDialog.shouldNot(present)
                } catch (failure: Throwable) {
                    runCatching { screenshot("failure.png") }.exceptionOrNull()?.let(failure::addSuppressed)
                    throw failure
                }
            }
            assertTrue(ideFailures.isEmpty(), "IDE reported failures:\n${ideFailures.joinToString("\n")}")
        } finally {
            di = originalDi
        }
    }

    private fun assertReport(report: String, matched: Int, unmatchedProperties: Int) {
        val expectedLines = listOf(
            "Matched keys: $matched",
            "Config entries without a matching Java reference: 12",
            "@Value references without a matching config entry: 2",
            "Potentially missing @Value config (no default): 1",
            "@Value references with a default fallback: 1",
            "@ConfigurationProperties fields without a matching config entry: $unmatchedProperties"
        )
        expectedLines.forEach { assertTrue(it in report.lines(), "Missing report line: $it\n$report") }
        listOf("demo.service.url", "demo.max-retries", "demo.client.timeout-ms", "(default present)",
            "config [default]", "config [prod]", "DemoProperties.Client#timeoutMs").forEach {
            assertTrue(report.contains(it), "Missing mapping detail: $it")
        }
        listOf("DEMO_DEFAULT_DO_NOT_USE", "SAMPLE_ONLY_NO_JAVA_REFERENCE", "UI_SAMPLE_REGION",
            "http://localhost:", "https://service.example.invalid").forEach {
            assertFalse(report.contains(it), "Report exposed a sample value: $it")
        }
    }

    private fun assertChangedConfigurationReport(report: String) {
        listOf(
            "Changed entries: 5",
            "Added: 0",
            "Modified: 5",
            "Removed: 0",
            "Deterministic risk findings: 5",
            "[CRITICAL] [SCG001] spring.jpa.hibernate.ddl-auto",
            "[HIGH] [SCG002] management.endpoints.web.exposure.include",
            "[HIGH] [SCG003] server.error.include-stacktrace",
            "[WARNING] [SCG004] logging.level.root",
            "[WARNING] [SCG005] spring.jpa.show-sql"
        ).forEach { assertTrue(report.contains(it), "Missing changed-config report detail: $it\n$report") }
        listOf("=create", "=*", "=always", "=DEBUG", "=true", "can modify", "may disclose").forEach {
            assertFalse(report.contains(it), "Changed-config report exposed a value or rule description: $it")
        }
    }

    private fun assertCommitWarning(warning: String) {
        listOf(
            "Risky Spring configuration changes detected",
            "5 deterministic finding(s)",
            "highest severity: CRITICAL",
            "The commit will continue.",
        ).forEach { assertTrue(warning.contains(it), "Missing commit warning detail: $it\n$warning") }
        listOf(
            "spring.jpa.hibernate.ddl-auto",
            "management.endpoints.web.exposure.include",
            "application-prod.properties",
            "=create",
            "=*",
            "=always",
            "=DEBUG",
            "=true",
        ).forEach { assertFalse(warning.contains(it), "Commit warning exposed configuration content: $it") }
    }

    private fun unpackSample(): Path {
        ZipFile(System.getProperty("scg.sample.zip")).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val target = temporaryDirectory.resolve(entry.name).normalize()
                require(target.startsWith(temporaryDirectory)) { "Invalid sample archive entry" }
                if (entry.isDirectory) {
                    Files.createDirectories(target)
                } else {
                    Files.createDirectories(target.parent)
                    zip.getInputStream(entry).use { Files.copy(it, target) }
                }
            }
        }
        return temporaryDirectory.resolve("config-mapping")
    }

    private fun initializeGitBaseline(project: Path) {
        val config = project.resolve("src/main/resources/application-prod.properties")
        val riskyContent = Files.readString(config)
        val safeContent = riskyContent
            .replace("spring.jpa.hibernate.ddl-auto=create", "spring.jpa.hibernate.ddl-auto=validate")
            .replace("management.endpoints.web.exposure.include=*", "management.endpoints.web.exposure.include=health")
            .replace("server.error.include-stacktrace=always", "server.error.include-stacktrace=never")
            .replace("logging.level.root=DEBUG", "logging.level.root=INFO")
            .replace("spring.jpa.show-sql=true", "spring.jpa.show-sql=false")
        check(safeContent != riskyContent) { "Risky sample substitutions were not applied" }

        Files.writeString(config, safeContent)
        runGit(project, "init", "--quiet")
        runGit(project, "config", "user.name", "Spring Config Guard CI")
        runGit(project, "config", "user.email", "spring-config-guard@example.invalid")
        runGit(project, "add", ".")
        runGit(project, "commit", "--quiet", "-m", "Safe sample baseline")
        Files.writeString(config, riskyContent)
    }

    private fun runGit(project: Path, vararg arguments: String) {
        val process = ProcessBuilder(listOf("git", *arguments))
            .directory(project.toFile())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { "git ${arguments.joinToString(" ")} failed:\n$output" }
    }

    private fun gitOutput(project: Path, vararg arguments: String): String {
        val process = ProcessBuilder(listOf("git", *arguments))
            .directory(project.toFile())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { "git ${arguments.joinToString(" ")} failed:\n$output" }
        return output
    }

    private fun screenshot(name: String) {
        Files.createDirectories(artifacts)
        val screen = Robot().createScreenCapture(Rectangle(Toolkit.getDefaultToolkit().screenSize))
        check(ImageIO.write(screen, "png", artifacts.resolve(name).toFile()))
    }

    companion object {
        private const val ACTION_ID = "SpringConfigGuard.AnalyzeConfigKeyMappings"
        private const val REPORT_TITLE = "Spring Config Guard - Key Mapping"
        private const val CHANGED_CONFIG_ACTION_ID = "SpringConfigGuard.AnalyzeChangedConfigDiff"
        private const val CHANGED_CONFIG_REPORT_TITLE = "Spring Config Guard - Changed Configuration"
        private const val COMMIT_PROJECT_ACTION_ID = "CheckinProject"
        private const val COMMIT_MESSAGE = "Verify non-blocking Spring Config Guard warning"
        private const val CONFIG_PATH = "src/main/resources/application-prod.properties"
    }
}

@Remote("java.awt.Dialog")
interface AwtDialog {
    fun isModal(): Boolean
}

@Remote("javax.swing.text.JTextComponent")
interface ReportTextComponent {
    fun isEditable(): Boolean
}

@Remote("javax.swing.JLabel")
interface AwtLabel {
    fun getText(): String
}

@Remote("javax.swing.text.JTextComponent")
interface AwtTextComponent {
    fun getText(): String
}
