# Getting started

Spring Config Guard analyzes Spring Boot configuration locally inside IntelliJ IDEA. It does not upload
project source or configuration, and its commit warning never blocks a commit.

## Install

Version `0.1.0` is currently pending its initial JetBrains Marketplace review. Until the listing is
public, install a successful CI artifact by following the [local installation guide](LOCAL_TESTING.md).
After publication, install **Spring Config Guard** from **Settings → Plugins → Marketplace**.

The plugin supports IntelliJ IDEA 2025.3 and later. CI verifies 2025.3.3 and 2026.1.3.

## Use the inspections

1. Open a project that contains Spring Boot `application.yml`, `application.yaml`, or
   `application.properties` files, including profile variants such as `application-prod.yml`.
2. Open a configuration file. Production-risk findings appear as standard editor warnings.
3. Review the rule ID and severity, then use the [rule reference](RULES.md) to understand the condition.

Detection is deterministic and profile-aware. Findings identify risk indicators; they do not prove that
a deployment is vulnerable.

## Run project analyses

Use the IntelliJ **Tools** menu to run the local reports:

- **Spring Config Guard: Analyze Profile Drift** compares effective profile entries.
- **Spring Config Guard: Analyze Config Key Mappings** maps configuration keys to Java `@Value` and
  `@ConfigurationProperties` references.
- **Spring Config Guard: Analyze Changed Configuration** reads locally changed Spring configuration and
  reports value-free diff and finding metadata.

Reports are snapshots. Run an action again after changing project files. Values and placeholder defaults
are omitted from reports.

## Commit warning

The plugin can show a value-free aggregate warning when selected commit changes contain deterministic
findings. It never cancels the commit. Disable or re-enable it per project under
**Settings → Tools → Spring Config Guard**; the manual analyses remain available.

For expected results, supported scope, and troubleshooting, see the [local testing guide](LOCAL_TESTING.md).
For bugs and feature requests, see [Support](../SUPPORT.md).
