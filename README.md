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
- `SCG006` Error-message exposure
- `SCG007` Binding-error exposure
- `SCG008` H2 console enabled in production
- `SCG009` Unsanitized Actuator environment values
- `SCG010` Unsanitized Actuator configuration-property values
- `SCG011` Actuator health details exposed to every user
- `SCG012` Actuator health components exposed to every user

## Architecture principle

Detection must be deterministic. AI is never the source of truth for risk detection; it is reserved for explanation and remediation.

## Build and try locally

Use **JDK 21** and the checked-in **Gradle 9.0.0 Wrapper**. No system Gradle installation is required;
the first run downloads Gradle and build dependencies. IntelliJ IDEA **2026.1.3** is the current test target.

```bash
./gradlew test verifyPluginProjectConfiguration verifyPluginStructure buildPlugin verifyPlugin buildSmokeTestSample
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
sample in the IDE, checks a fresh report, and validates the five value-free changed-configuration
findings against a disposable local Git baseline. It also commits those changes through IntelliJ,
checks the value-free warning, and verifies that the warning did not prevent the local commit. Test
reports, screenshots, and available IDE logs are retained in `spring-config-guard-ui-tests-...`
artifacts. Run `./gradlew integrationTest`
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

Early MVP with production-risk inspections, profile drift analysis, local config/Java key mapping, and
a deterministic configuration-diff core. **Tools → Spring Config Guard: Analyze Changed Configuration**
checks project-relative paths before reading revision content, reads only locally available VCS
revisions for changed Spring application configuration, and reports the active-rule count,
added/modified/removed entry counts, and deterministic finding metadata without exposing values.
If a supported revision cannot be read, analysis stops with a generic local error instead of
treating it as missing or returning a partial report. The active-rule count is shown even when there
are no changes or findings, and the
report warns when every rule is disabled. A finding shows only its rule ID, severity, key, profile,
and file/line location. It does not upload
source/configuration or block commits. The commit-precheck core can recommend a local review from
value-free aggregate metadata. IntelliJ runs this check against the changes selected for a local
commit and shows an aggregate warning when review is recommended, but it never cancels the commit.
The warning-only commit check is enabled by default and can be disabled per project under
**Settings → Tools → Spring Config Guard**. The same project-local page lets you enable or disable
individual SCG001–SCG012 rules, or use **Enable all rules**, **Disable all rules**, and
**Reset rules to defaults**. Rule controls are independent of the commit-warning toggle; resetting
rules does not change that toggle. See [Rule catalog](docs/RULES.md) for each check. This setting does
not disable the manual changed-configuration analysis action. The warning notification's **Configure…** action opens that project settings page
directly.

## Getting started and support

- [Getting Started](docs/GETTING_STARTED.md)
- [Support and Resources](docs/SUPPORT.md)
- [Local testing guide](docs/LOCAL_TESTING.md)
- [Marketplace release guide](docs/MARKETPLACE_RELEASE.md)
- [JetBrains Marketplace page](https://plugins.jetbrains.com/plugin/34237-spring-config-guard)
- [GitHub Issues](https://github.com/tjdgus903/spring-config-guard/issues)

## Release status

Version `0.1.0` has been manually submitted to JetBrains Marketplace and is currently pending JetBrains review.
The direct listing is [Spring Config Guard on JetBrains Marketplace](https://plugins.jetbrains.com/plugin/34237-spring-config-guard).
Submission does not establish approval or public availability; JetBrains controls review status and Marketplace publication.

The manually triggered signed-artifact workflow and release checklist are documented in
[docs/MARKETPLACE_RELEASE.md](docs/MARKETPLACE_RELEASE.md). No workflow publishes the plugin automatically.
