# HubPilot

HubPilot is a Minecraft server-network suite for Velocity + Paper/Bukkit. It was created based on the [AutoServer](https://github.com/artificial-720/AutoServer) plugin and expanded into a larger system for server control, navigation, management, telemetry, and hub tools.

> **Current release:** HubPilot 1.0.0

## Project background

HubPilot was created and is maintained by `ohHeyItsCon`. The project idea, feature direction, behavior, testing decisions, and release decisions come from that work.

AI tools have been used heavily during development for coding, debugging, code review, testing, and documentation. More detail is available in [TRANSPARENCY.md](TRANSPARENCY.md).

For controller testing, **Crafty Controller is the only external server-management controller covered by live beta testing for 1.0.0**. Pterodactyl and Generic HTTP support are included and have controlled test coverage, but they do not yet have the same live testing history.

## Components

| Component | Install on | Purpose |
| --- | --- | --- |
| **HubPilot Core** | Velocity proxy | Routing, server requests, provider control, discovery, setup, permissions, status, statistics, and `/hp` commands |
| **HubPilot Hub** | Hub Paper/Bukkit server | Navigator GUI, admin GUI, setup screens, staff bridge, and hub items/settings |
| **HubPilot Interact** | Hub Paper/Bukkit server | Entity, sign, portal, and supported mannequin server bindings |
| **HubPilot Link** | Hub Paper/Bukkit server | HubPilot link/telemetry bridge used by the hub |

Backend game servers do not need HubPilot JARs for normal routing, server requests, or provider power control.

## Versioning and component updates

HubPilot uses one unified version number across Core, Hub, Interact, and Link. Every public HubPilot release belongs to one suite version so it is clear which component builds were released and tested together.

**Core and Hub should stay on the exact same HubPilot version.** These two components handle the main proxy-to-hub behavior and should be updated together.

Link and Interact do not need to be replaced every time a release changes only Core or Hub. Release notes will state when an existing Link or Interact build remains compatible and when an update is required.

As a general compatibility limit, Link and Interact should not be more than **two published HubPilot releases behind** Core and Hub. If a release explicitly says that Link or Interact must be updated, that requirement overrides the two-release grace period.

## Main features

- Configurable server Navigator
- Offline server requests with retries and countdowns
- Crafty Controller power control and live discovery
- Pterodactyl and Generic HTTP provider adapters
- Always-On mode for servers HubPilot should not power-manage
- Server start/stop controls when a power provider is configured
- Idle and failed-request shutdown for resource saving
- Per-server version and compatibility rules
- Owner, Admin, Moderator, and Helper roles
- In-game admin tools and protected HubPilot items
- Entity, sign, portal, and supported mannequin bindings
- HubPilot telemetry through Link
- `/hub` and `/lobby` routing
- Compatibility with [ViaVersion](https://github.com/ViaVersion/ViaVersion)
- Compatibility with [LuckPerms](https://github.com/LuckPerms/LuckPerms) for hub-side permission nodes

## Quick install

### 1. Velocity proxy

Place `HubPilot-Core-1.0.0.jar` in the Velocity proxy's `plugins` folder.

```text
velocity/plugins/HubPilot-Core-1.0.0.jar
```

### 2. Hub server

Place the hub-side JARs in the **hub Paper/Bukkit server's** `plugins` folder.

```text
hub/plugins/HubPilot-Hub-1.0.0.jar
hub/plugins/HubPilot-Interact-1.0.0.jar
hub/plugins/HubPilot-Link-1.0.0.jar
```

`HubPilot-Hub` and `HubPilot-Link` belong on the hub. `HubPilot-Interact` is optional if entity, sign, portal, or mannequin bindings are not needed.

Do not keep multiple versions of the same HubPilot component in one `plugins` folder.

### 3. Keep the hub online

The recommended setup is one lightweight hub server that stays online while larger backend servers are allowed to shut down when nobody is using them.

A common layout is:

```text
Hub       -> Always-On
Survival  -> Crafty / another power provider
Modded    -> Crafty / another power provider
Minigames -> Crafty / another power provider
```

This keeps a permanent landing point available for players without requiring every game server to stay running.

### 4. Give each server behind Velocity its own port

Every Minecraft server registered behind Velocity needs its own listening port.

Using one range makes the setup easier to read and troubleshoot. For example:

```text
Velocity public port: 25565

Hub:                25600
Survival:           25601
Modded Survival:    25602
Minigames:          25603
```

The `2560x` range is only a recommended convention. Any unused ports will work.

When players always enter through Velocity, backend ports normally do not need to be exposed directly to the internet.

### 5. Finish setup

Restart Velocity first, then restart the hub and backend servers.

Join the configured hub as an operator and run:

```text
/hp claimowner
/hp setup
```

Choose the provider that matches how the servers are hosted. If that provider uses an API key or token, open [Server Providers](docs/PROVIDERS.md) and follow the setup section for that provider before adding servers.

After the provider is connected, `/hp discover` and the admin GUI can be used to add and manage destinations.

For the full walkthrough, read [Installation](docs/INSTALLATION.md).

## Provider options

### Crafty Controller

Use Crafty when HubPilot should start and stop servers through Crafty Controller. This is the controller path with live beta testing and the most real-network coverage.

### Always-On

Always-On means HubPilot leaves server power management to another system.

It works well for:

- the hub server
- servers that stay online 24/7
- paid hosts that handle startup themselves
- Docker or systemd servers managed outside HubPilot
- hosts without an API HubPilot can use

Always-On does not require a controller URL or API key. If the server is offline, HubPilot cannot start it.

### Pterodactyl / Generic HTTP

Both adapters are included for other hosting setups. They have controlled test coverage but have not yet received the same live beta testing as Crafty.

See [Server Providers](docs/PROVIDERS.md) for the API-key and endpoint setup for Crafty, Pterodactyl, and Generic HTTP.

## ViaVersion and server versions

HubPilot is compatible with [ViaVersion](https://github.com/ViaVersion/ViaVersion).

If Velocity and ViaVersion can connect a client to a backend server, HubPilot can still request that server, start it when a power provider is configured, and route the player to it. HubPilot does not add a separate protocol restriction unless a strict version rule is set for that destination.

Modded servers may still require a matching Minecraft version, loader, or modpack even when the underlying protocol is supported by ViaVersion.

## LuckPerms

HubPilot is compatible with [LuckPerms](https://github.com/LuckPerms/LuckPerms).

LuckPerms can be used on the Paper/Bukkit hub to grant HubPilot permission nodes such as `hubpilot.admin`, `hubpilot.navigator`, and the Interact permission nodes. HubPilot's built-in Owner/Admin/Moderator/Helper system still works without LuckPerms, so LuckPerms is optional rather than a dependency.

HubPilot Owner is still managed by HubPilot itself and is not automatically granted by a LuckPerms group.

## Main commands

- `/hp` or `/hubpilot` - main HubPilot command
- `/hp gui` - open the admin GUI
- `/hp adminitem` - give or repair the HubPilot admin item
- `/hp claimowner` - claim a fresh installation
- `/hp setup` - first-time/provider setup
- `/hp staff` - staff management
- `/hp providers` - provider information
- `/hp request <server>` - request a managed server
- `/hp discover` - find eligible Velocity backends
- `/hp discover add <server>` - import a discovered backend
- `/hpi` or `/hubpilotinteract` - interaction tools
- `/hpl` or `/hubpilotlink` - Link information
- `/hub` and `/lobby` - return to the hub

See [Commands](docs/COMMANDS.md) for the full command list.

## Compatibility and testing

- Core runs on Velocity.
- Hub, Interact, and Link target the Bukkit/Paper 1.21 API family.
- Release-candidate field testing used Velocity 4.1.0-SNAPSHOT build 16.
- Crafty Controller is the only external server-management controller covered by live beta testing for 1.0.0.
- HubPilot works alongside ViaVersion for backends supported by the installed ViaVersion setup.
- HubPilot works with LuckPerms for hub-side Bukkit/Paper permission nodes.
- Native mannequin support depends on the server exposing the required modern Bukkit/Paper capability.

Other versions and hosting setups may work, but they are not listed as tested until they have actually been tested.

## Documentation

- [Installation](docs/INSTALLATION.md)
- [Server Providers](docs/PROVIDERS.md)
- [Configuration](docs/CONFIGURATION.md)
- [Permissions and Roles](docs/PERMISSIONS.md)
- [Commands](docs/COMMANDS.md)
- [FAQ and Troubleshooting](docs/FAQ.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Future Roadmap](docs/ROADMAP.md)
- [Release Validation](docs/VALIDATION.md)
- [Project Transparency](TRANSPARENCY.md)
- [Security](SECURITY.md)

## License

HubPilot is released under the [MIT License](LICENSE).

HubPilot was created based on AutoServer by Artificial-720. The original AutoServer MIT notice is retained in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and inside the release JARs.
