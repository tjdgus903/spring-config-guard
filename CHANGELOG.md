# Changelog

All notable user-facing changes to Spring Config Guard are documented here.

## 0.1.0 — 2026-09-12

- Added deterministic inspections for five risky Spring Boot production settings.
- Added local profile-drift analysis, including inherited local endpoint detection.
- Added local configuration-to-Java mapping for literal `@Value` placeholders and supported
  `@ConfigurationProperties` fields and record components.
- Added value-free changed-configuration reports for locally available VCS revisions.
- Added optional per-project commit warnings that report only aggregate finding metadata and never
  block a local commit.
- Added automated unit, structure, compatibility, archive, sample, and real-IDE UI verification.

Spring Config Guard does not upload project source or configuration. Version 0.1.0 does not include
Marketplace publication automation or optional AI explanations.
