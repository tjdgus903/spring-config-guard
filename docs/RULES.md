# Rules

All risk detection is deterministic. Unless noted otherwise, MVP rules apply only to configuration classified as production.

## SCG001 — Hibernate ddl-auto in production

Property: `spring.jpa.hibernate.ddl-auto`

| Value | Severity | Rationale |
|---|---|---|
| `create` | CRITICAL | May recreate schema on startup |
| `create-drop` | CRITICAL | Creates and drops schema during lifecycle |
| `update` | WARNING | Mutates schema automatically and may be unsuitable for controlled production migrations |
| `validate` | none | Validation only |
| `none` | none | No automatic schema action |

## SCG002 — Wildcard Actuator web exposure

Property: `management.endpoints.web.exposure.include`

- Severity: HIGH
- Match: a comma/list token exactly equal to `*`
- Safe examples for this rule: `health`, `health,info`
- Rationale: wildcard inclusion can make every available web endpoint eligible for exposure unless separately restricted.

## SCG003 — Stacktrace always included

Property: `server.error.include-stacktrace`

- Severity: HIGH
- Match: value `always` (case-insensitive)
- Rationale: stacktraces may disclose application internals in error responses.

## SCG004 — Root DEBUG logging

Property: `logging.level.root`

- Severity: WARNING
- Match: value `DEBUG` (case-insensitive)
- Rationale: root DEBUG may create excessive logs and disclose runtime details.

## SCG005 — Hibernate show-sql

Property: `spring.jpa.show-sql`

- Severity: WARNING
- Match: value `true` (case-insensitive)
- Rationale: SQL is written to standard output and is generally unsuitable as a production diagnostic mechanism.

## SCG006 — Error message always included

Property: `server.error.include-message`

- Severity: HIGH
- Match: value `always` (case-insensitive, surrounding whitespace ignored)
- Safe examples for this rule: `never`, `on_param`
- Rationale: exception messages may disclose internal application details in error responses.

## SCG007 — Binding errors always included

Property: `server.error.include-binding-errors`

- Severity: HIGH
- Match: value `always` (case-insensitive, surrounding whitespace ignored)
- Safe examples for this rule: `never`, `on_param`
- Rationale: binding errors may disclose validation and application model details in error responses.

## SCG008 — H2 console enabled in production

Property: `spring.h2.console.enabled`

- Severity: HIGH
- Match: value `true` (case-insensitive, surrounding whitespace ignored)
- Safe example for this rule: `false`
- Rationale: Spring Boot documents the H2 web console as development-only and advises against enabling it in production.

## SCG009 — Unsanitized Actuator environment values

Property: `management.endpoint.env.show-values`

- Severity: HIGH
- Match: value `always` (case-insensitive, surrounding whitespace ignored)
- Safe examples for this rule: `never`, `when-authorized`
- Rationale: `always` shows unsanitized values from the Actuator `env` endpoint to every user.

## SCG010 — Unsanitized Actuator configuration-property values

Property: `management.endpoint.configprops.show-values`

- Severity: HIGH
- Match: value `always` (case-insensitive, surrounding whitespace ignored)
- Safe examples for this rule: `never`, `when-authorized`
- Rationale: `always` shows unsanitized values from the Actuator `configprops` endpoint to every user.

## SCG011 — Actuator health details exposed to every user

Property: `management.endpoint.health.show-details`

- Severity: HIGH
- Match: value `always` (case-insensitive, surrounding whitespace ignored)
- Safe examples for this rule: `never`, `when-authorized`
- Rationale: `always` exposes component health details to every user who can access the health endpoint.

## SCG012 — Actuator health components exposed to every user

Property: `management.endpoint.health.show-components`

- Severity: HIGH
- Match: value `always` (case-insensitive, surrounding whitespace ignored)
- Safe examples for this rule: `never`, `when-authorized`
- Rationale: `always` exposes component health information to every user who can access the health endpoint.

## SCG013 — Actuator shutdown endpoint enabled in production

Property: `management.endpoint.shutdown.enabled`

- Severity: HIGH
- Match: value `true` (case-insensitive, surrounding whitespace ignored)
- Safe example for this rule: `false`
- Rationale: explicitly enabling the shutdown endpoint allows application shutdown through Actuator if that endpoint is exposed. This rule flags the enablement as a review-worthy production risk without inferring the surrounding authorization or exposure configuration.

## SCG014 — Immediate server shutdown in production

Property: `server.shutdown`

- Severity: WARNING
- Match: value `immediate` (case-insensitive, surrounding whitespace ignored)
- Safe example for this rule: `graceful`
- Rationale: immediate shutdown skips graceful request draining and can terminate in-flight requests during application shutdown.

## Profile drift analysis

Profile drift is modeled separately from single-file production rules. The analyzer resolves each named profile against the default `application.*` configuration and records whether each effective value is explicit or inherited.

Ordinary overrides are emitted as informational `DIFFERENCE` findings. Deterministic risk rules are emitted separately as `RISK` findings with a rule ID.

### SCG-PD001 — Production inherits a local development endpoint

- Severity: HIGH
- Applies only when a production profile inherits the value from the default configuration.
- Key must be endpoint-like (`.url`, `.uri`, `.host`, `.hostname`, `.endpoint`, `.base-url`, `.baseurl`).
- Value must point to an explicit local endpoint such as `localhost`, `127.0.0.1`, or `::1`.
- An explicit production override suppresses this finding.
- Rationale: a production profile that silently inherits a development endpoint can start successfully while routing traffic to the wrong destination.

## Scope note

These findings are risk indicators, not proof that a deployment is vulnerable or will fail. Later versions may incorporate surrounding Spring Security and deployment context while preserving deterministic detection.
