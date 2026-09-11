# Spring Config Guard

Detect risky Spring Boot production configuration before deployment.

## Goal

Spring Config Guard is an IntelliJ IDEA plugin that detects dangerous or inconsistent Spring Boot configuration before code is committed or deployed.

## MVP scope

- Spring Boot `application.yml` / `application.properties`
- Production profile detection
- Rule-based risk detection
- IDE inspection warnings
- Profile drift detection
- `@Value` / `@ConfigurationProperties` mapping
- Git diff guard
- Optional BYOK AI explanations and remediation

## Initial rule set

- `SCG001` Hibernate `ddl-auto` risky production values
- `SCG002` Actuator wildcard exposure
- `SCG003` Stacktrace exposure
- `SCG004` Root DEBUG logging
- `SCG005` `show-sql=true`

## Architecture principle

Detection must be deterministic. AI is never the source of truth for risk detection; it is reserved for explanation and remediation.

## Config / Java key mapping

Open a project with Java source roots and Spring Boot `application*.yml`, `application*.yaml`, or
`application*.properties` files. After indexing finishes, select **Tools → Spring Config Guard:
Analyze Config Key Mappings**.

The action analyzes current editor content in the background and opens a scrollable, copyable report
with exact-key matches, unmatched occurrences, profiles, and file/line locations. The report is a
snapshot: run the action again after editing. Repeated requests replace an in-flight analysis.
Configuration values and `@Value` expressions/default text are omitted. Each section shows up to
20 items, each matched key shows up to 5 locations per kind, and omitted counts are displayed.

Supported Java references are literal Spring `@Value` placeholders and the current
`@ConfigurationProperties` field extractor, including directly referenced static nested classes.
Unmatched occurrences are informational. This is an inventory across project modules/profiles
(including test sources), not a reconstruction of Spring's effective runtime binding. Relaxed-name
equivalence, environment/external property sources, constructor/record/Kotlin binding, and active
profile or module isolation are not resolved. Java files outside source roots, libraries, and excluded
content are not scanned. Malformed or unreadable configuration files are skipped; an empty result
does not establish that no configuration exists.

## Status

Early MVP with production-risk inspections, profile drift analysis, and local config/Java key mapping.
