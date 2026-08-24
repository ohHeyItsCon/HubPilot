# HubPilot

HubPilot is a Minecraft server-network suite for Velocity + Paper/Bukkit. It started on top of [AutoServer](https://github.com/artificial-720/AutoServer) and grew into a larger system for server navigation, on-demand startup, provider control, hub management, and telemetry.

> **Current release:** HubPilot 1.0.0

## What HubPilot does

HubPilot can keep a lightweight hub online while larger backend servers stay off until somebody actually requests them. It also works fine on networks that keep servers running all the time, since power management is optional.

Main features include:

- configurable server Navigator
- server requests with retries and countdowns
- Crafty Controller, Pterodactyl, Generic HTTP, and Always-On providers
- idle and failed-request shutdown for resource saving
- per-server version rules
- Owner, Admin, Moderator, and Helper roles
- in-game admin tools
- entity, sign, portal, and mannequin bindings through Interact
- hub-side telemetry through Link
- `/hub` and `/lobby` routing
- ViaVersion compatibility
- LuckPerms support for hub-side permission nodes

## Components

| Component | Install on | What it handles |
| --- | --- | --- |
| **HubPilot Core** | Velocity proxy | Routing, requests, providers, discovery, setup, permissions, status, statistics, and `/hp` commands |
| **HubPilot Hub** | Hub Paper/Bukkit server | Navigator, admin GUI, setup, staff tools, and hub items |
| **HubPilot Interact** | Hub Paper/Bukkit server | Entity, sign, portal, and supported mannequin bindings |
| **HubPilot Link** | Hub Paper/Bukkit server | Communication and telemetry between the hub and Core |

Backend game servers do not need HubPilot JARs for normal routing, requests, or provider power control.

## Quick install

Put Core on Velocity:

```text
velocity/plugins/HubPilot-Core-1.0.0.jar
```

Put Hub and Link on the Paper/Bukkit hub. Interact is optional:

```text
hub/plugins/HubPilot-Hub-1.0.0.jar
hub/plugins/HubPilot-Link-1.0.0.jar
hub/plugins/HubPilot-Interact-1.0.0.jar
```

Restart Velocity, restart the hub, join as an operator, then run:

```text
/hp claimowner
/hp setup
```

A common resource-saving layout is:

```text
Hub       -> Always-On
Survival  -> Crafty / another power provider
Modded    -> Crafty / another power provider
Minigames -> Crafty / another power provider
```

Every server behind Velocity still needs its own listening port. The full setup, including provider API keys, port examples, discovery, ViaVersion, and LuckPerms, is covered in [Installation](docs/INSTALLATION.md) and [Server Providers](docs/PROVIDERS.md).

## Versioning

All four HubPilot plugins share one public version number.

**Core and Hub should always run the same HubPilot version.** Link and Interact can stay on an older build when the release notes say that build is still compatible, but they should not be more than **two published HubPilot releases behind** Core and Hub.

If a release says Link or Interact must be updated, that requirement overrides the normal two-release window.

## Compatibility and testing

Core runs on Velocity. Hub, Interact, and Link target the Bukkit/Paper 1.21 API family.

Crafty Controller is the only external controller with live beta coverage for 1.0.0. Pterodactyl and Generic HTTP are included and have controlled testing behind them, but not the same live network coverage yet.

HubPilot works alongside [ViaVersion](https://github.com/ViaVersion/ViaVersion) and supports [LuckPerms](https://github.com/LuckPerms/LuckPerms) for normal hub-side permission nodes.

## Documentation

- [Installation](docs/INSTALLATION.md)
- [Server Providers](docs/PROVIDERS.md)
- [Configuration](docs/CONFIGURATION.md)
- [Permissions and Roles](docs/PERMISSIONS.md)
- [Commands](docs/COMMANDS.md)
- [FAQ](docs/FAQ.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
- [Project Transparency](TRANSPARENCY.md)
- [1.0.0 Validation Record](release/1.0.0/VALIDATION.txt)
- [Security](SECURITY.md)

## License and project history

HubPilot is released under the [MIT License](LICENSE).

HubPilot was built on top of AutoServer by Artificial-720. The original AutoServer MIT notice is kept in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and inside the release JARs.

AI tools have been used heavily during development for coding, debugging, testing, code review, and documentation. More detail is in [TRANSPARENCY.md](TRANSPARENCY.md).
