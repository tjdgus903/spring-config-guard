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

## Status

Early MVP bootstrap.
