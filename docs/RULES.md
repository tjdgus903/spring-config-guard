# Rules

## SCG001 — Hibernate ddl-auto in production

Property: `spring.jpa.hibernate.ddl-auto`

| Value | Severity | Rationale |
|---|---|---|
| `create` | CRITICAL | May recreate schema on startup |
| `create-drop` | CRITICAL | Creates and drops schema during lifecycle |
| `update` | WARNING | Mutates schema automatically and may be unsuitable for controlled production migrations |
| `validate` | none | Validation only |
| `none` | none | No automatic schema action |

The rule applies only when the analyzed configuration is classified as a production profile.

## Planned MVP rules

- `SCG002`: wildcard Actuator exposure in production
- `SCG003`: stacktrace exposure in production
- `SCG004`: root DEBUG logging in production
- `SCG005`: SQL logging via `spring.jpa.show-sql=true` in production
