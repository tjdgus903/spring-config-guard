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
import org.junit.jupiter.api.Assertions.assertEquals
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

                    // Mutate the open IDE document while the nonmodal report is still present.
                    frame.toFront()
                    val editor = frame.codeEditor().shouldBe(present)
                    assertTrue(editor.isEditable(), "Sample editor must remain editable")
                    editor.text = editor.text.trimEnd() + "\ndemo.region=UI_SAMPLE_REGION\n"
                    assertTrue(editor.text.contains("demo.region=UI_SAMPLE_REGION"))
                    assertEquals(initialReport, reportArea.text, "An existing report remains a snapshot")
                    reportDialog.toFront()
                    reportDialog.button("Close").click()
                    reportDialog.shouldNot(present)

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

    private fun screenshot(name: String) {
        Files.createDirectories(artifacts)
        val screen = Robot().createScreenCapture(Rectangle(Toolkit.getDefaultToolkit().screenSize))
        check(ImageIO.write(screen, "png", artifacts.resolve(name).toFile()))
    }

    companion object {
        private const val ACTION_ID = "SpringConfigGuard.AnalyzeConfigKeyMappings"
        private const val REPORT_TITLE = "Spring Config Guard - Key Mapping"
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
