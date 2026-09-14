# Support and Resources

Spring Config Guard is an open-source IntelliJ IDEA plugin maintained in this repository.

## Bug reports and feature requests

Use GitHub Issues:

https://github.com/tjdgus903/spring-config-guard/issues

Before opening an issue, search existing issues first. For reproducible problems, include:

- IntelliJ IDEA product and exact version
- Spring Config Guard version
- operating system and JDK when relevant
- configuration format (`.yml`, `.yaml`, or `.properties`)
- minimal reproduction steps
- expected and actual behavior

Do not include passwords, tokens, private keys, production secrets, or configuration values that should remain confidential.

## Documentation

- Getting Started: `docs/GETTING_STARTED.md`
- Local build and IDE smoke testing: `docs/LOCAL_TESTING.md`
- Marketplace release process: `docs/MARKETPLACE_RELEASE.md`
- Config mapping sample and expectations: `samples/config-mapping/README.md`

## Marketplace

Plugin page:

https://plugins.jetbrains.com/plugin/34237-spring-config-guard

Version `0.1.0` has been submitted for initial JetBrains Marketplace review. Repository documentation must not be interpreted as JetBrains approval or public availability until the Marketplace status changes.

## Security and privacy

Risk detection is deterministic and local. Project source and Spring configuration are not uploaded by the core inspection, profile-drift, mapping, changed-configuration, or commit-warning flows. Reports intentionally avoid configuration values.

If a future security issue requires private disclosure rather than a public issue, do not include exploit secrets or production credentials in a public ticket. Use the repository owner's GitHub contact path to coordinate a private disclosure channel.