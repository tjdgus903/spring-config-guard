package io.github.tjdgus903.springconfigguard.project;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import io.github.tjdgus903.springconfigguard.code.ConfigUsage;
import io.github.tjdgus903.springconfigguard.code.ConfigurationPropertyMapping;
import io.github.tjdgus903.springconfigguard.code.SpringConfigurationPropertiesPsiExtractor;
import io.github.tjdgus903.springconfigguard.code.SpringValuePsiExtractor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Collects supported Spring references from Java source roots, including test sources. */
public final class ProjectJavaConfigCollector {
    private final SpringValuePsiExtractor valueExtractor = new SpringValuePsiExtractor();
    private final SpringConfigurationPropertiesPsiExtractor propertiesExtractor =
            new SpringConfigurationPropertiesPsiExtractor();

    /** Caller must hold read access in smart mode with committed PSI documents. */
    public ProjectJavaConfigReferences collect(Project project) {
        ProjectFileIndex index = ProjectFileIndex.getInstance(project);
        List<VirtualFile> files = new ArrayList<>();
        index.iterateContent(file -> {
            ProgressManager.checkCanceled();
            if (!file.isDirectory() && index.isInSourceContent(file)
                    && file.getFileType() == JavaFileType.INSTANCE) {
                files.add(file);
            }
            return true;
        });
        files.sort(Comparator.comparing(VirtualFile::getPath));

        List<ConfigUsage> usages = new ArrayList<>();
        List<ConfigurationPropertyMapping> mappings = new ArrayList<>();
        PsiManager psiManager = PsiManager.getInstance(project);
        for (VirtualFile file : files) {
            ProgressManager.checkCanceled();
            PsiFile psiFile = psiManager.findFile(file);
            if (psiFile != null) {
                usages.addAll(valueExtractor.extract(psiFile));
                mappings.addAll(propertiesExtractor.extract(psiFile));
            }
        }
        return new ProjectJavaConfigReferences(usages, mappings);
    }
}
