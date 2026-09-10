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

## Scope note

These findings are risk indicators, not proof that a deployment is vulnerable or will fail. Later versions may incorporate surrounding Spring Security and deployment context while preserving deterministic detection.
