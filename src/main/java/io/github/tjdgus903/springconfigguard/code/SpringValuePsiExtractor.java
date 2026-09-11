package io.github.tjdgus903.springconfigguard.code;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiAnnotationMemberValue;

import java.util.ArrayList;
import java.util.List;

/** Extracts Spring {@code @Value} placeholder usages from Java PSI. */
public final class SpringValuePsiExtractor {
    private static final String SPRING_VALUE = "org.springframework.beans.factory.annotation.Value";

    public List<ConfigUsage> extract(PsiFile file) {
        if (!(file instanceof PsiJavaFile)) {
            return List.of();
        }

        List<ConfigUsage> usages = new ArrayList<>();
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        String filePath = file.getVirtualFile() != null ? file.getVirtualFile().getPath() : file.getName();

        file.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitAnnotation(PsiAnnotation annotation) {
                super.visitAnnotation(annotation);
                if (!SPRING_VALUE.equals(annotation.getQualifiedName())) {
                    return;
                }

                PsiAnnotationMemberValue memberValue = annotation.findAttributeValue("value");
                if (!(memberValue instanceof PsiLiteralExpression literal)) {
                    return;
                }
                Object value = literal.getValue();
                if (!(value instanceof String expression)) {
                    return;
                }

                int line = 1;
                if (document != null) {
                    line = document.getLineNumber(annotation.getTextOffset()) + 1;
                }

                for (SpringPlaceholderParser.PlaceholderReference placeholder : SpringPlaceholderParser.parse(expression)) {
                    usages.add(new ConfigUsage(
                            placeholder.key(),
                            placeholder.defaultValue(),
                            expression,
                            filePath,
                            line
                    ));
                }
            }
        });
        return List.copyOf(usages);
    }
}
