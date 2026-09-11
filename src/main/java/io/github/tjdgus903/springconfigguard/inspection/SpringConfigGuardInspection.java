package io.github.tjdgus903.springconfigguard.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import io.github.tjdgus903.springconfigguard.model.Finding;
import io.github.tjdgus903.springconfigguard.rule.ConfigContext;
import io.github.tjdgus903.springconfigguard.rule.MvpRuleRegistry;
import io.github.tjdgus903.springconfigguard.rule.RuleEngine;
import io.github.tjdgus903.springconfigguard.scanner.ConfigFileScanner;
import io.github.tjdgus903.springconfigguard.scanner.ConfigProfile;
import io.github.tjdgus903.springconfigguard.scanner.ConfigProfileDetector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * IntelliJ adapter for deterministic Spring Config Guard findings.
 *
 * <p>The inspection layer is intentionally thin: parsing and risk decisions stay in the scanner and
 * rule engine, while this class only maps findings to editor elements.</p>
 */
public final class SpringConfigGuardInspection extends LocalInspectionTool {
    private final ConfigFileScanner scanner = new ConfigFileScanner();
    private final ConfigProfileDetector profileDetector = new ConfigProfileDetector();
    private final RuleEngine ruleEngine = MvpRuleRegistry.ruleEngine();

    @Override
    public boolean runForWholeFile() {
        // The deterministic scanner needs the complete config document. Spring configuration files
        // are typically small, and this keeps the MVP correct while the PSI-specific visitor evolves.
        return true;
    }

    @Override
    public boolean isAvailableForFile(@NotNull PsiFile file) {
        return profileDetector.detect(file.getName())
                .map(ConfigProfile::production)
                .orElse(false);
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder,
            boolean isOnTheFly
    ) {
        Optional<ConfigProfile> profile = profileDetector.detect(holder.getFile().getName());
        if (profile.isEmpty() || !profile.get().production()) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        ConfigProfile productionProfile = profile.get();
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                registerFindings(file, productionProfile, holder);
            }
        };
    }

    private void registerFindings(
            PsiFile file,
            ConfigProfile profile,
            ProblemsHolder holder
    ) {
        List<ConfigEntry> entries;
        try {
            entries = scanner.scan(file.getText(), file.getName(), profile.name());
        } catch (RuntimeException ignored) {
            // Syntax errors are handled by the IDE/language plugin. A malformed document must not
            // make this inspection fail or flood the editor with secondary findings.
            return;
        }

        ConfigContext context = new ConfigContext(true);
        for (ConfigEntry entry : entries) {
            for (Finding finding : ruleEngine.analyze(entry, context)) {
                PsiElement target = locateTarget(file, finding.entry());
                holder.registerProblem(
                        target,
                        formatMessage(finding),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                );
            }
        }
    }

    private PsiElement locateTarget(PsiFile file, ConfigEntry entry) {
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null || document.getLineCount() == 0) {
            return file;
        }

        int lineIndex = Math.max(0, Math.min(entry.line() - 1, document.getLineCount() - 1));
        int lineStart = document.getLineStartOffset(lineIndex);
        int lineEnd = document.getLineEndOffset(lineIndex);
        String lineText = document.getText(new TextRange(lineStart, lineEnd));

        int absoluteOffset = lineStart;
        if (entry.value() != null && !entry.value().isEmpty()) {
            int valueOffset = lineText.indexOf(entry.value());
            if (valueOffset >= 0) {
                absoluteOffset += valueOffset;
            }
        }

        if (absoluteOffset >= file.getTextLength() && file.getTextLength() > 0) {
            absoluteOffset = file.getTextLength() - 1;
        }

        PsiElement element = file.findElementAt(absoluteOffset);
        return element != null ? element : file;
    }

    private String formatMessage(Finding finding) {
        return "[" + finding.ruleId() + "][" + finding.severity() + "] " + finding.message();
    }
}
