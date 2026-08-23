# HubPilot 1.0.0

HubPilot 1.0.0 is the first public release of the project.

HubPilot was created based on the [AutoServer](https://github.com/artificial-720/AutoServer) plugin and expanded into a larger suite for server navigation, power control, management, telemetry, and hub interactions.

The project was created and is maintained by `ohHeyItsCon`. AI tools were used heavily during development for implementation, debugging, testing, code inspection, and documentation.

**Crafty Controller is the only external server-management controller covered by live beta testing for this release.** Pterodactyl and Generic HTTP support are included, but do not yet have the same live testing coverage.

## Components

- `HubPilot-Core-1.0.0.jar` - Velocity routing, server requests, providers, discovery, setup, permissions, status, and commands
- `HubPilot-Hub-1.0.0.jar` - Navigator/admin UI, setup/staff bridge, and hub settings
- `HubPilot-Interact-1.0.0.jar` - entity, sign, portal, and supported mannequin bindings
- `HubPilot-Link-1.0.0.jar` - hub-side Link/telemetry bridge

## Main features

- in-game server Navigator and administration
- Crafty-backed on-demand startup, shutdown, and discovery
- Always-On mode for servers HubPilot should not power-manage
- Pterodactyl and Generic HTTP adapters for other hosting setups
- offline request, retry, and countdown flow
- idle and failed-request shutdown for resource saving
- role-based HubPilot administration
- hub/world interaction bindings
- HubPilot telemetry through Link
- compatibility with [ViaVersion](https://github.com/ViaVersion/ViaVersion)
- compatibility with [LuckPerms](https://github.com/LuckPerms/LuckPerms) for hub-side permissions

## Recommended layout

Keep a small hub server online all the time and use Always-On for it. Larger backend servers can then stay offline until requested and be started through Crafty or another power provider.

Servers behind Velocity should each use a unique internal port. A `2560x` layout such as `25600`, `25601`, and `25602` is an easy convention, but any unused ports will work.

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

All 411 packaged classes and 2,753 concrete method bodies passed JDK ASM BasicVerifier after the 1.0.0 release rebase. The final JARs also pass archive integrity and main-class checks.

The full release validation record is in `release/1.0.0/VALIDATION.txt`.

## Testing scope

The 1.0.0 code payload is based on the validated 3.5.18 release candidate. The previous 3.5.18 report listed three deployment-specific live checks that were not separately recorded as completed. The maintainer approved proceeding with the 1.0.0 public release after the final version-rebase and bytecode validation.

## Installation and troubleshooting

See [Installation](../../docs/INSTALLATION.md), [Providers](../../docs/PROVIDERS.md), and the [FAQ](../../docs/FAQ.md) before configuring a public server.

## License

HubPilot is released under the MIT License. AutoServer's original MIT notice is retained in the repository and release JARs.
