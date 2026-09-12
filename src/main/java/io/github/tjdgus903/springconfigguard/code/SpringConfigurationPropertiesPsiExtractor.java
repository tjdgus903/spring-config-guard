package io.github.tjdgus903.springconfigguard.code;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiRecordComponent;
import com.intellij.psi.PsiType;

import java.util.ArrayList;
import java.util.List;

/** Extracts field and record-component bindings from Spring Boot {@code @ConfigurationProperties} classes. */
public final class SpringConfigurationPropertiesPsiExtractor {
    private static final String CONFIGURATION_PROPERTIES =
            "org.springframework.boot.context.properties.ConfigurationProperties";

    public List<ConfigurationPropertyMapping> extract(PsiFile file) {
        if (!(file instanceof PsiJavaFile)) {
            return List.of();
        }

        List<ConfigurationPropertyMapping> mappings = new ArrayList<>();
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        String filePath = file.getVirtualFile() != null ? file.getVirtualFile().getPath() : file.getName();

        file.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitClass(PsiClass psiClass) {
                PsiAnnotation annotation = psiClass.getAnnotation(CONFIGURATION_PROPERTIES);
                if (annotation != null) {
                    String prefix = readPrefix(annotation);
                    if (prefix != null) {
                        collectBindings(psiClass, prefix, filePath, document, mappings);
                    }
                }
                super.visitClass(psiClass);
            }
        });

        return List.copyOf(mappings);
    }

    private static String readPrefix(PsiAnnotation annotation) {
        PsiAnnotationMemberValue memberValue = annotation.findDeclaredAttributeValue("prefix");
        if (memberValue == null) {
            memberValue = annotation.findDeclaredAttributeValue("value");
        }
        if (memberValue == null) {
            return "";
        }
        if (!(memberValue instanceof PsiLiteralExpression literal)) {
            return null;
        }
        Object value = literal.getValue();
        return value instanceof String stringValue ? stringValue.trim() : null;
    }

    private static void collectBindings(
            PsiClass psiClass,
            String prefix,
            String filePath,
            Document document,
            List<ConfigurationPropertyMapping> mappings
    ) {
        String declaringClass = qualifiedOrSimpleName(psiClass);

        if (psiClass.isRecord()) {
            for (PsiRecordComponent component : psiClass.getRecordComponents()) {
                PsiClass nestedClass = resolveDirectStaticNestedClass(psiClass, component.getType());
                if (nestedClass != null) {
                    String nestedPrefix = SpringPropertyKey.withPrefix(prefix, component.getName());
                    collectBindings(nestedClass, nestedPrefix, filePath, document, mappings);
                    continue;
                }

                addMapping(prefix, declaringClass, component.getName(), component.getTextOffset(), filePath, document, mappings);
            }
            return;
        }

        for (PsiField field : psiClass.getFields()) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) {
                continue;
            }

            PsiClass nestedClass = resolveDirectStaticNestedClass(psiClass, field.getType());
            if (nestedClass != null) {
                String nestedPrefix = SpringPropertyKey.withPrefix(prefix, field.getName());
                collectBindings(nestedClass, nestedPrefix, filePath, document, mappings);
                continue;
            }

            addMapping(prefix, declaringClass, field.getName(), field.getTextOffset(), filePath, document, mappings);
        }
    }

    private static void addMapping(
            String prefix,
            String declaringClass,
            String memberName,
            int textOffset,
            String filePath,
            Document document,
            List<ConfigurationPropertyMapping> mappings
    ) {
        int line = 1;
        if (document != null) {
            line = document.getLineNumber(textOffset) + 1;
        }

        mappings.add(new ConfigurationPropertyMapping(
                SpringPropertyKey.withPrefix(prefix, memberName),
                prefix,
                declaringClass,
                memberName,
                filePath,
                line
        ));
    }

    private static PsiClass resolveDirectStaticNestedClass(PsiClass owner, PsiType type) {
        if (!(type instanceof PsiClassType classType)) {
            return null;
        }

        PsiClass resolved = classType.resolve();
        if (resolved == null) {
            return null;
        }
        if (resolved.getContainingClass() != owner) {
            return null;
        }
        if (!resolved.hasModifierProperty(PsiModifier.STATIC) && !resolved.isRecord()) {
            return null;
        }
        return resolved;
    }

    private static String qualifiedOrSimpleName(PsiClass psiClass) {
        String declaringClass = psiClass.getQualifiedName();
        if (declaringClass == null || declaringClass.isBlank()) {
            declaringClass = psiClass.getName();
        }
        if (declaringClass == null || declaringClass.isBlank()) {
            declaringClass = "<anonymous>";
        }
        return declaringClass;
    }
}
