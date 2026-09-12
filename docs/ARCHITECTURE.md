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
duplicate entries and never renders values itself. The local IntelliJ VCS adapter reads available
before/after revisions for changed `application*.properties`, `application*.yml`, and
`application*.yaml` files and supplies their parsed entries to this core. It performs no remote VCS
operation, source/config upload, or commit blocking. A separate pure analysis layer evaluates the
existing deterministic rules only against added and modified current entries; it retains the source
change with each finding. The local report renders only rule ID, severity, key, profile, and
file/line metadata; it never renders configuration values, prior values, or raw revision content.
A pure precheck policy reduces these findings to a value-free finding count and highest severity. It
can only recommend proceeding or reviewing before proceeding; it has no reject/block outcome. The
IntelliJ commit adapter applies that policy only to the changes selected for a local commit. A review
recommendation produces a value-free warning notification and still returns no commit problem, so
clean, risky, cancelled, and failed analyses all leave the commit unblocked. CI adapters remain future work.
