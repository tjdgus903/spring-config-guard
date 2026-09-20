# Security Policy

Spring Config Guard analyzes Spring Boot configuration locally inside IntelliJ IDEA. Security and
privacy reports are welcome, but sensitive material must not be posted in public issues.

## Supported versions

Until a newer release is published, security fixes target the current `0.1.x` release line and the
latest `main` branch where applicable.

| Version | Supported |
| --- | --- |
| 0.1.x | Yes |
| Older/unreleased development snapshots | Best effort |

## Reporting a vulnerability

Do not open a public issue containing:

- passwords, API tokens, private keys, signing material, or other credentials;
- production configuration values, customer data, or proprietary source code;
- identity, banking, tax, payout, or Marketplace account-verification data;
- exploit details that would make an unpatched vulnerability materially easier to abuse.

If **Report a vulnerability** is available on this repository's **Security** tab, use that private
GitHub flow. If it is not available, contact the repository owner through GitHub without including
sensitive details and ask to establish a private disclosure channel.

For a security report, provide only the minimum information needed to reproduce and assess the issue:
affected Spring Config Guard version, IntelliJ IDEA version, operating system when relevant, impact,
and minimal reproduction steps. Redact configuration values and credentials.

Ordinary bugs, feature requests, and non-sensitive false positives should use
[GitHub Issues](https://github.com/tjdgus903/spring-config-guard/issues).

## Response and disclosure

Reports will be assessed against the currently supported release line. A fix may be prepared and
verified before public technical details are discussed. No response-time or remediation-time SLA is
promised by this open-source project.

Do not treat repository activity, a signed artifact, or a Marketplace entry as confirmation that a
security fix has been published. Verify the released plugin version through JetBrains Marketplace
before relying on a Marketplace update.
