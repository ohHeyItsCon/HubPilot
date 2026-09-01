# HubPilot

HubPilot is a Minecraft server-network suite for Velocity + Paper/Bukkit. It started on top of [AutoServer](https://github.com/artificial-720/AutoServer) and grew into a larger system for server navigation, on-demand startup, provider control, hub management, and telemetry.

> **Current pre-release:** HubPilot 1.0.2 — Queue Update
>
> **Current stable release:** HubPilot 1.0.1

## What HubPilot does

HubPilot can keep a lightweight hub online while larger backend servers stay off until somebody actually requests them. It also works fine on networks that keep servers running all the time, since power management is optional.

Main features include:

- configurable server Navigator
- live Navigator telemetry updates while the GUI stays open
- server requests with retries and countdowns
- configurable join and queue messages with global and per-server controls
- queue position and cancellation feedback
- Crafty Controller, Pterodactyl, Generic HTTP, and Always-On providers
- per-server Always-On lifecycle option for managed servers
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
velocity/plugins/HubPilot-Core-1.0.2.jar
```

Put Hub and Link on the Paper/Bukkit hub. Interact is optional:

```text
hub/plugins/HubPilot-Hub-1.0.2.jar
hub/plugins/HubPilot-Link-1.0.2.jar
hub/plugins/HubPilot-Interact-1.0.2.jar
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

Starting with 1.0.1, a Crafty/Pterodactyl/Generic HTTP server can also be marked **Always-On server** in its Automation Settings. HubPilot can still start it through the provider if needed, but it will not automatically stop that server for idle time, failed requests, or an empty request queue.

Navigator server items also update their telemetry while the menu stays open. HubPilot refreshes the open Navigator after each complete status sync, using the existing status cycle instead of adding another repeating task.

Every server behind Velocity still needs its own listening port. The full setup, including provider API keys, port examples, discovery, ViaVersion, and LuckPerms, is covered in [Installation](docs/INSTALLATION.md) and [Server Providers](docs/PROVIDERS.md).

## Versioning

All four HubPilot plugins share one public version number.

**Core and Hub should always run the same HubPilot version.** Link and Interact can stay on an older build when the release notes say that build is still compatible, but they should not be more than **two published HubPilot releases behind** Core and Hub.

For 1.0.2, Core and Hub need to be updated together. Link and Interact have no functional changes, so their 1.0.1 builds remain compatible. Matching 1.0.2 builds are provided for consistent suite versioning.

## Compatibility and testing

Core runs on Velocity. Hub, Interact, and Link target the Bukkit/Paper 1.21 API family.

Crafty Controller is the only external controller with live beta coverage from the 1.0.0 testing cycle. Pterodactyl and Generic HTTP are included and have controlled testing behind them, but not the same live network coverage yet.

The 1.0.1 Always-On path passed packaged and live testing without blocking manual Stop Server. Live Navigator testing confirmed startup progress from 0% to 100%, changing ping, Online status, countdown, and player transfer without reopening the menu. Crafty discovery, duplicate migration, shared layouts, and Admin editing were also tested on the final release build.

The live Navigator refresh uses the existing status-sync path. Exact packaged-code tests confirmed that open Navigator inventories update in place and refresh once after `STATUS_SYNC_END` instead of once for every server row.

HubPilot works alongside [ViaVersion](https://github.com/ViaVersion/ViaVersion) and supports [LuckPerms](https://github.com/LuckPerms/LuckPerms) for normal hub-side permission nodes.

## Documentation

- [Installation](docs/INSTALLATION.md)
- [Server Providers](docs/PROVIDERS.md)
- [Configuration](docs/CONFIGURATION.md)
- [Permissions and Roles](docs/PERMISSIONS.md)
- [Commands](docs/COMMANDS.md)
- [FAQ](docs/FAQ.md)
- [Known Bugs](KNOWN_BUGS.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
- [Project Transparency](TRANSPARENCY.md)
- [1.0.1 Validation Record](release/1.0.1/VALIDATION.txt)
- [1.0.1 Release Notes](release/1.0.1/RELEASE-NOTES.md)
- [1.0.2 Pre-release Notes](release/1.0.2/RELEASE-NOTES.md)
- [1.0.2 Validation Record](release/1.0.2/VALIDATION.txt)
- [Security](SECURITY.md)

## License and project history

HubPilot is released under the [MIT License](LICENSE).

HubPilot was built on top of AutoServer by Artificial-720. The original AutoServer MIT notice is kept in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and inside the release JARs.

AI tools have been used heavily during development for coding, debugging, testing, code review, and documentation. More detail is in [TRANSPARENCY.md](TRANSPARENCY.md).
