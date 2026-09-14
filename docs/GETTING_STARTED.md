# Getting Started

Spring Config Guard analyzes Spring Boot configuration locally inside IntelliJ IDEA. It does not upload project source or configuration, and its commit check is advisory only.

## 1. Install the plugin

After JetBrains approves the Marketplace listing, install **Spring Config Guard** from **Settings → Plugins → Marketplace** and restart the IDE if prompted.

For local development builds, use **Settings → Plugins → Install Plugin from Disk** with the ZIP produced under `build/distributions/`.

## 2. Open a Spring Boot project

Open a project containing one or more Spring Boot configuration files:

- `application.yml`
- `application.yaml`
- `application.properties`
- profile variants such as `application-prod.yml`

Wait for IntelliJ indexing to finish before running project-wide analysis actions.

## 3. Review production-risk inspections

Spring Config Guard highlights deterministic production risks directly in supported Spring configuration files. Current rules include risky Hibernate schema generation, Actuator exposure, verbose error disclosure, H2 console exposure, SQL logging, and health/configuration detail exposure.

The inspection reports the rule, severity, key, profile, and location without exposing the configuration value in reports or logs.

## 4. Analyze profile drift

Use **Tools → Spring Config Guard: Analyze Profile Drift**.

The report compares local Spring profile configuration and identifies deterministic drift patterns such as inherited local endpoints. The analysis is local and does not attempt to reconstruct every external or runtime property source.

## 5. Map configuration keys to Java

Use **Tools → Spring Config Guard: Analyze Config Key Mappings**.

The report connects local configuration keys with supported Java references, including literal `@Value` placeholders and the current `@ConfigurationProperties` field/record-component extractor. It shows matching and unmatched occurrences with file/line locations while omitting configuration values and `@Value` defaults.

Double-click a rendered `config`, `@Value`, or `@ConfigurationProperties` file/line location to open that project file at the reported line. Headings, key labels, omitted-count lines, invalid or missing files, directories, and paths outside the current project are ignored.

## 6. Analyze changed configuration

Use **Tools → Spring Config Guard: Analyze Changed Configuration**.

The action reads locally available VCS revisions and reports changed Spring configuration using value-free metadata. No source or configuration is uploaded.

## 7. Optional commit warning

The plugin can show a value-free warning before a local commit when changed configuration warrants review. The warning never blocks or cancels the commit.

The setting is enabled by default and can be changed per project under **Settings → Tools → Spring Config Guard**.

## Support

- Issues and bug reports: https://github.com/tjdgus903/spring-config-guard/issues
- Source and documentation: https://github.com/tjdgus903/spring-config-guard
- Marketplace page: https://plugins.jetbrains.com/plugin/34237-spring-config-guard

When reporting a problem, include the IDE version, plugin version, configuration file format, and reproduction steps. Do not post secrets or production configuration values.
