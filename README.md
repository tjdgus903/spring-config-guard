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

## Build and try locally

Use **JDK 21** and the checked-in **Gradle 9.0.0 Wrapper**. No system Gradle installation is required;
the first run downloads Gradle and build dependencies. IntelliJ IDEA **2026.1.3** is the current test target.

```bash
./gradlew test verifyPluginProjectConfiguration verifyPluginStructure buildPlugin buildSmokeTestSample
./gradlew -p samples/config-mapping classes
./gradlew runIde
```

On Windows use `.\gradlew.bat` in place of `./gradlew`. The installable plugin ZIP is under
`build/distributions/`; the standalone sample (including its Wrapper) is
`build/smoke-test/spring-config-guard-sample.zip`. Unpack the sample and open `config-mapping` in the IDE
launched by `runIde`.

Successful [CI runs](https://github.com/tjdgus903/spring-config-guard/actions/workflows/ci.yml) also retain
plugin and sample ZIP artifacts for 14 days. Extract the outer GitHub artifact archive, then install
the inner plugin ZIP with **Settings → Plugins → Install Plugin from Disk**.

See the [Windows/Unix execution and installation guide](docs/LOCAL_TESTING.md) and the
[sample's expected results and manual checklist](samples/config-mapping/README.md).
CI also runs a real IDE smoke test: it installs the built ZIP, checks the mapping report, edits the
sample in the IDE, and checks a fresh report. Test reports, before/after screenshots, and available
IDE logs are retained in `spring-config-guard-ui-tests-...` artifacts. Run `./gradlew integrationTest`
locally with JDK 21 and a desktop session. Checks on your own IDE installation remain separate.

## Config / Java key mapping

Open a project with Java source roots and Spring Boot `application*.yml`, `application*.yaml`, or
`application*.properties` files. After indexing finishes, select **Tools → Spring Config Guard:
Analyze Config Key Mappings**.

The action analyzes current editor content in the background and opens a scrollable, copyable report
with matching keys, unmatched occurrences, profiles, and file/line locations. The report is a
snapshot: run the action again after editing. Repeated requests replace an in-flight analysis.
Configuration values and `@Value` expressions/default text are omitted. Each section shows up to
20 items, each matched key shows up to 5 locations per kind, and omitted counts are displayed.

Supported Java references are literal Spring `@Value` placeholders and the current
`@ConfigurationProperties` field and Java record-component extractor, including directly referenced nested classes and records.
Matching supports case, hyphen, and underscore variants within the same dot-separated key segment;
it never treats a hyphen as a hierarchy separator. Unmatched config and
`@ConfigurationProperties` occurrences are informational. An unmatched literal `@Value` reference without
a default is shown as potentially missing, not as a runtime failure. This is an inventory across project modules/profiles
(including test sources), not a reconstruction of Spring's effective runtime binding. Environment/external
property sources, regular constructor-bound classes/Kotlin binding, and active
profile or module isolation are not resolved. Java files outside source roots, libraries, and excluded
content are not scanned. Malformed or unreadable configuration files are skipped; an empty result
does not establish that no configuration exists.

## Status

Early MVP with production-risk inspections, profile drift analysis, and local config/Java key mapping.
efa23a318d9a7737fc3c21e5b942caf2a634b628
