package io.github.tjdgus903.springconfigguard.project;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import io.github.tjdgus903.springconfigguard.mapping.ConfigFileJavaKeyMatcher;
import io.github.tjdgus903.springconfigguard.mapping.ConfigKeyMappingAnalysis;
import io.github.tjdgus903.springconfigguard.model.ConfigEntry;

import java.util.List;

/** IntelliJ adapter connecting local project sources to the independent relaxed-key matching core. */
public final class ProjectConfigKeyMappingAnalyzer {
    private final ProjectConfigCollector configCollector = new ProjectConfigCollector();
    private final ProjectJavaConfigCollector javaCollector = new ProjectJavaConfigCollector();
    private final ConfigFileJavaKeyMatcher matcher = new ConfigFileJavaKeyMatcher();

    /**
     * Caller must use a smart-mode read action with committed PSI documents. Results describe
     * project-wide key occurrences across profiles/modules, not effective runtime bindings.
     */
    public ConfigKeyMappingAnalysis analyze(Project project) {
        List<ConfigEntry> entries = configCollector.collect(project);
        ProjectJavaConfigReferences references = javaCollector.collect(project);
        ProgressManager.checkCanceled();
        return matcher.match(entries, references.valueUsages(), references.propertyMappings());
    }
}
