# HubPilot 1.0.0

HubPilot 1.0.0 is the first public release of the full HubPilot suite.

HubPilot started on top of [AutoServer](https://github.com/artificial-720/AutoServer) and grew into a larger system for server navigation, power control, management, telemetry, and hub interactions.

The project is created and maintained by `ohHeyItsCon`. AI tools were used heavily for coding, debugging, testing, code inspection, and documentation.

Crafty Controller is the only external server-management controller with live beta coverage for this release. Pterodactyl and Generic HTTP are included and have controlled test coverage, but they have not had the same live-network testing yet.

## Components

- `HubPilot-Core-1.0.0.jar` - Velocity routing, server requests, providers, discovery, setup, permissions, status, and commands
- `HubPilot-Hub-1.0.0.jar` - Navigator/admin UI, setup/staff bridge, and hub settings
- `HubPilot-Interact-1.0.0.jar` - entity, sign, portal, and supported mannequin bindings
- `HubPilot-Link-1.0.0.jar` - hub-side communication and telemetry

## Main features

- in-game server Navigator and admin tools
- Crafty-backed on-demand startup, shutdown, and discovery
- Always-On provider for externally managed servers
- Pterodactyl and Generic HTTP providers
- offline request, retry, and countdown flow
- idle and failed-request shutdown for resource saving
- HubPilot roles and permissions
- entity, sign, portal, and mannequin bindings
- HubPilot telemetry through Link
- ViaVersion compatibility
- LuckPerms support for hub-side permission nodes

## Recommended layout

A simple setup keeps one lightweight hub online all the time and lets larger backends stay offline until somebody asks for them.

Every server behind Velocity needs its own internal port. `25600`, `25601`, and `25602` are an easy convention, but any unused ports work.

## Release integrity

Final SHA-256 hashes:

```text
HubPilot-Core-1.0.0.jar
b5368cf4cc94941889225a1c6950d17dccdd8309589ef997de56d7ef1af11220

HubPilot-Hub-1.0.0.jar
525b7294f8513adf52f5f83bac77854b5e3d024a4f1d2e8b0bd1a0e30ddbf96d

HubPilot-Interact-1.0.0.jar
83f3260ba9e202dee0ec935b0624ca1a79d71f338f26fe0a93191a21ba42f961

HubPilot-Link-1.0.0.jar
91a878246c939eabcb9a422caa36a42f2ea982cd2c8e6b18f62581e2b3525b23

HubPilot-1.0.0.zip
2676bbd9e7bd451aee9d4ce5720e966f39bf3190a19f8e1f7ddb1e621376e015
```

All 411 packaged classes and 2,753 concrete method bodies passed JDK ASM BasicVerifier after the 1.0.0 version rebase. The final JARs also passed archive integrity and main-class checks.

The full validation record is in `release/1.0.0/VALIDATION.txt`.

## Testing scope

The 1.0.0 code payload comes from the validated 3.5.18 release candidate. The older report listed a few deployment-specific live checks that were not separately recorded as completed. The release still went through the final version, archive, metadata, and bytecode checks before publication.

## Installation and troubleshooting

See [Installation](../../docs/INSTALLATION.md), [Providers](../../docs/PROVIDERS.md), and the [FAQ](../../docs/FAQ.md).

## License

HubPilot is MIT licensed. AutoServer's original MIT notice is kept in the repo and inside the release JARs.
