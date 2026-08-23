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

## Recommended layout

Keep a small hub server online all the time and use Always-On for it. Larger backend servers can then stay offline until requested and be started through Crafty or another power provider.

Servers behind Velocity should each use a unique internal port. A `2560x` layout such as `25600`, `25601`, and `25602` is an easy convention, but any unused ports will work.

## ViaVersion

HubPilot works alongside ViaVersion. When Velocity/ViaVersion can connect a client to a backend, HubPilot can request and route to that backend unless a strict HubPilot version rule blocks the destination.

Modded servers may still require a matching Minecraft version, loader, or modpack.

## Always-On

Always-On does not start or stop the server process. Use it when the server is already managed by a host, Docker, systemd, another panel, or another process manager.

If an Always-On server is offline, HubPilot cannot bring it online. Start it through the system that owns the process.

## Installation and troubleshooting

See [Installation](../../docs/INSTALLATION.md), [Providers](../../docs/PROVIDERS.md), and the [FAQ](../../docs/FAQ.md) before configuring a public server.

## Release integrity

SHA-256 hashes for the final 1.0.0 JARs will be added after those exact artifacts are built and validated.
