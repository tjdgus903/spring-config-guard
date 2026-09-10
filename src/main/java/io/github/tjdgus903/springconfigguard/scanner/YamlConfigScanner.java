package io.github.tjdgus903.springconfigguard.scanner;

import io.github.tjdgus903.springconfigguard.model.ConfigEntry;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.AnchorNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/** Parses YAML into flattened Spring property keys while retaining source line information. */
public final class YamlConfigScanner {

    public List<ConfigEntry> scan(String content, String filePath, String profile) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));

        List<ConfigEntry> entries = new ArrayList<>();
        for (Node root : yaml.composeAll(new StringReader(content))) {
            if (root != null) {
                flatten(root, "", filePath, profile, entries);
            }
        }
        return List.copyOf(entries);
    }

    private void flatten(
            Node node,
            String prefix,
            String filePath,
            String profile,
            List<ConfigEntry> entries
    ) {
        if (node instanceof AnchorNode anchorNode) {
            flatten(anchorNode.getRealNode(), prefix, filePath, profile, entries);
            return;
        }

        if (node instanceof MappingNode mappingNode) {
            for (NodeTuple tuple : mappingNode.getValue()) {
                if (!(tuple.getKeyNode() instanceof ScalarNode keyNode)) {
                    continue;
                }

                String key = join(prefix, keyNode.getValue());
                Node valueNode = tuple.getValueNode();
                if (valueNode instanceof MappingNode || valueNode instanceof AnchorNode) {
                    flatten(valueNode, key, filePath, profile, entries);
                } else if (valueNode instanceof ScalarNode scalarNode) {
                    entries.add(new ConfigEntry(
                            key,
                            scalarNode.getValue(),
                            profile,
                            filePath,
                            scalarNode.getStartMark().getLine() + 1
                    ));
                } else if (valueNode instanceof SequenceNode sequenceNode) {
                    addSequence(sequenceNode, key, filePath, profile, entries);
                }
            }
        }
    }

    private void addSequence(
            SequenceNode sequenceNode,
            String key,
            String filePath,
            String profile,
            List<ConfigEntry> entries
    ) {
        List<String> scalarValues = new ArrayList<>();
        boolean allScalar = true;
        int firstLine = sequenceNode.getStartMark().getLine() + 1;

        for (Node child : sequenceNode.getValue()) {
            if (child instanceof ScalarNode scalarNode) {
                scalarValues.add(scalarNode.getValue());
                if (scalarValues.size() == 1) {
                    firstLine = scalarNode.getStartMark().getLine() + 1;
                }
            } else {
                allScalar = false;
                break;
            }
        }

        if (allScalar) {
            entries.add(new ConfigEntry(
                    key,
                    String.join(",", scalarValues),
                    profile,
                    filePath,
                    firstLine
            ));
            return;
        }

        for (int i = 0; i < sequenceNode.getValue().size(); i++) {
            flatten(sequenceNode.getValue().get(i), key + "[" + i + "]", filePath, profile, entries);
        }
    }

    private String join(String prefix, String key) {
        return prefix == null || prefix.isBlank() ? key : prefix + "." + key;
    }
}
