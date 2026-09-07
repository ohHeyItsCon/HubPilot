# HubPilot

HubPilot manages a Velocity Minecraft network from its Paper/Bukkit hub. Players can request an offline server, wait while it starts, and join when it's ready. Servers can shut down when they're no longer needed, or stay online if that's how you run your network.

> **Stable:** [1.0.1](https://github.com/ohHeyItsCon/HubPilot/releases/tag/v1.0.1)  
> **Prerelease:** [1.0.2: Queue Update](release/1.0.2/RELEASE-NOTES.md)

## Features

- Server Navigator with live status, player counts, ping, and startup progress
- On-demand startup, retries, countdowns, and automatic shutdown
- Crafty Controller, Pterodactyl, Generic HTTP, and Always-On providers
- Per-server Always-On option that keeps provider startup and manual stop available
- In-game server and Navigator editing
- Owner, Admin, Moderator, and Helper roles
- Entity, sign, portal, and supported mannequin bindings through Interact
- Per-server version rules, ViaVersion compatibility, and LuckPerms permission support
- `/hub` and `/lobby` routing

The **1.0.2 prerelease** adds editable join and queue messages, queue positions, Interact editing tools and labels, and a repair for automatic hub shutdown. See its [release notes](release/1.0.2/RELEASE-NOTES.md) for the builds to use and the live tests still pending.

## Components

| Plugin | Install on | What it handles |
| --- | --- | --- |
| Core | Velocity proxy | Routing, requests, power providers, discovery, roles, status, and `/hp` commands |
| Hub | Paper/Bukkit hub | Navigator, admin menus, setup, and hub items |
| Interact | Paper/Bukkit hub | Entity, sign, portal, and supported mannequin bindings |
| Link | Paper/Bukkit hub | Communication and telemetry between the hub and Core |

Backend game servers don't need HubPilot JARs for normal requests, routing, or provider power control.

> [!IMPORTANT]
> **Fabric server setup:** Every Fabric backend using Velocity modern forwarding with HubPilot needs [FabricProxy-Lite](https://github.com/OKTW-Network/FabricProxy-Lite). Install a build that supports the backend's Minecraft version, along with its required dependencies.
>
> Set `secret` in the backend's `config/FabricProxy-Lite.toml` to the contents of Velocity's forwarding secret file. The file is selected by `forwarding-secret-file` in `velocity.toml` and is usually named `forwarding.secret`. Velocity must use `player-info-forwarding-mode = "modern"`. An empty or mismatched secret will prevent players from joining.
>
> This requirement applies to Fabric. Other mod loaders need forwarding support appropriate to their loader and version; Paper has its own forwarding settings. Players do not need FabricProxy-Lite on their clients, but they still need any mods required by the backend's modpack. See [Velocity's forwarding guide](https://docs.papermc.io/velocity/player-information-forwarding/).
>
> I'm working on automating the secret transfer so owners won't have to copy it by hand. It's planned for the [next major update after the 1.0.2 prerelease](docs/ROADMAP.md#next-major-update-prepare-for-velocity) and is not available yet.

## Quick install

These examples use stable **1.0.1**. For prerelease installs, follow the [1.0.2 update instructions](release/1.0.2/RELEASE-NOTES.md#which-files-to-replace).

Put Core in Velocity's `plugins` folder. Put Hub and Link in the Paper hub's `plugins` folder, plus Interact if you want world interactions. Keep only one version of each plugin.

Restart Velocity and the hub, join the configured hub as an operator, then run:

```text
/hp claimowner
/hp setup
```

Keep a hub online so players have somewhere to wait. Each backend needs an address and port that Velocity can reach. For the full setup, see [Installation](docs/INSTALLATION.md) and [Server Providers](docs/PROVIDERS.md).

## Updating

Core and Hub must run the same HubPilot version. Update them together.

Link and Interact can stay on an older build only when the release notes say it's compatible, and no more than **two published releases behind**. If a release requires an update, that takes priority.

The latest 1.0.2 Interact features need the newer Hub and Interact builds. Link 1.0.1 remains compatible. Several 1.0.2 JARs kept their filenames after repairs, so check the supplied SHA-256 list.

## Compatibility and testing

Core runs on Velocity. The hub-side plugins target the Bukkit/Paper 1.21 API family.

Crafty Controller has live beta coverage. Pterodactyl and Generic HTTP have controlled test coverage, but haven't had the same live network testing.

Always-On and live Navigator refresh passed live testing in 1.0.1. The full results are in the [1.0.1 notes](release/1.0.1/RELEASE-NOTES.md). **1.0.2 still has live tests pending.**

### Compatible plugins and mods

| Project / repository | Use with HubPilot | Requirements and limits |
| --- | --- | --- |
| [ViaVersion](https://github.com/ViaVersion/ViaVersion) | Connect newer clients to older servers through protocol translation. | HubPilot's strict version rules can still block a transfer. |
| [ViaBackwards](https://github.com/ViaVersion/ViaBackwards) | Let older clients join newer servers within its supported version range. | Requires ViaVersion. Newer blocks and items may appear as substitutes on older clients. |
| [LuckPerms](https://github.com/LuckPerms/LuckPerms) | Grant HubPilot permission nodes on the Paper/Bukkit hub. | Optional. HubPilot's own roles work without it, and Owner is still assigned by HubPilot. |
| [FabricProxy-Lite](https://github.com/OKTW-Network/FabricProxy-Lite) | Handle Velocity modern forwarding on Fabric backends. | Requires a compatible Minecraft build and the matching Velocity secret. See the setup notice above. |

Use versions that support your server software. This list does not mean every plugin version or modpack has been tested. Protocol translation does not resolve missing mods or modpack handshake failures.

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
- [Contributing](CONTRIBUTING.md)
- [Security](SECURITY.md)
- [1.0.1 Validation](release/1.0.1/VALIDATION.txt)
- [1.0.2 Validation](release/1.0.2/VALIDATION.txt)

## Project history and license

Created and maintained by `ohHeyItsCon`, HubPilot grew from [AutoServer](https://github.com/artificial-720/AutoServer) by Artificial-720. It uses the [MIT License](LICENSE), with the original notice kept in [Third-Party Notices](THIRD_PARTY_NOTICES.md) and the release JARs.

AI tools have been used for coding, debugging, testing, review, and documentation. See [Project Transparency](TRANSPARENCY.md) for how the project was developed.
