# Architecture

## Core flow

```text
IntelliJ PSI / config parser
        ↓
    ConfigEntry
        ↓
    RuleEngine
        ↓
     Finding
        ↓
IntelliJ Inspection / Tool Window / Git Guard
```

The core rule engine must remain independent from IntelliJ APIs so it can be unit-tested cheaply and reused later in a CLI or CI integration.

## Layers

- `model`: configuration entries and findings
- `rule`: deterministic rule contracts and engine
- `rule.rules`: concrete production-risk rules
- `inspection`: future IntelliJ inspection adapter
- `scanner`: future YAML/properties/profile extraction
- `diff`: deterministic local configuration delta analysis for future VCS, commit, and CI adapters
- `ai`: future optional BYOK explanation/remediation only

## Product boundaries

V0.x intentionally excludes Kubernetes, Helm, cloud secret managers, and remote services. These are future integrations after local Spring Boot configuration analysis proves useful.

## Git Diff Guard Foundation

The diff core compares parsed before/after configuration entries and classifies additions, value
modifications, and removals without calling Git, IntelliJ VCS APIs, or remote services. It preserves
duplicate entries and never renders values itself. An IntelliJ VCS action and pre-commit/CI adapters
will consume this core in later steps.
