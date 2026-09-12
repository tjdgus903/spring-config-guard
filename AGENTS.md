# Spring Config Guard Agent Instructions

## Product

Spring Config Guard is an IntelliJ IDEA plugin for deterministic detection of risky Spring Boot production configuration.

## Non-negotiable architecture rules

1. Keep the rule engine independent from IntelliJ Platform APIs.
2. Do not use AI to decide whether a configuration is dangerous.
3. AI may only explain findings or propose remediation.
4. Every new rule requires positive and negative tests.
5. Avoid IntelliJ internal APIs. Prefer documented public APIs.
6. Do not broaden a change beyond the assigned issue.
7. Preserve backwards compatibility with supported IDE versions unless an issue explicitly changes the baseline.

## Quality gate

Every change must pass:

```bash
gradle test
gradle verifyPluginProjectConfiguration
gradle verifyPluginStructure
gradle buildPlugin
gradle verifyPlugin
```

Plugin Verifier checks the built archive against the currently targeted IntelliJ Platform. Structure
validation and the real-IDE UI smoke test remain separate required checks.

## Rule contribution checklist

Each rule must include:

- stable rule ID (`SCGxxx`)
- severity
- exact property key or deterministic matching criteria
- production-only behavior where applicable
- positive fixture/test
- negative fixture/test
- documentation in `docs/RULES.md`

## Security and privacy

- Never upload project source or configuration automatically.
- BYOK credentials must not be logged.
- Any future AI request must send the minimum necessary context.
