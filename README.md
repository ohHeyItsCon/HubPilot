# HubPilot

HubPilot is a Minecraft server-network suite for Velocity + Paper/Bukkit. It started on top of [AutoServer](https://github.com/artificial-720/AutoServer) and grew into a much larger system for server control, navigation, management, telemetry, and hub tools.

> **Current release:** HubPilot 1.0.0

## Project background

HubPilot was created and is maintained by `ohHeyItsCon`. The project idea, feature direction, testing decisions, and release decisions come from that work.

AI tools have been used heavily for coding, debugging, testing, code review, and documentation. That is documented openly in [TRANSPARENCY.md](TRANSPARENCY.md).

Crafty Controller is the only external server-management controller that has been live beta tested for 1.0.0. Pterodactyl and Generic HTTP are included and have controlled test coverage, but they have not had the same live network testing yet.

## Components

| Component | Install on | What it handles |
| --- | --- | --- |
| **HubPilot Core** | Velocity proxy | Routing, server requests, providers, discovery, setup, permissions, status, statistics, and `/hp` commands |
| **HubPilot Hub** | Hub Paper/Bukkit server | Navigator GUI, admin GUI, setup, staff tools, and hub items |
| **HubPilot Interact** | Hub Paper/Bukkit server | Entity, sign, portal, and supported mannequin bindings |
| **HubPilot Link** | Hub Paper/Bukkit server | Hub-side communication and telemetry between HubPilot and Core |

Backend game servers do not need HubPilot JARs for normal routing, server requests, or provider power control.

## Versioning and component updates

All four HubPilot plugins share one release number. That keeps the suite on one version line and makes major compatibility changes easier to handle.

**Core and Hub should always run the same HubPilot version.** Update those two together.

Link and Interact do not need to be replaced every time a release only changes Core or Hub. The release notes will say when an older Link or Interact build is still compatible.

As a general rule, Link and Interact should not be more than **two published HubPilot releases behind** Core and Hub. If a release says one of them must be updated, follow that requirement even if it is still inside the normal two-release window.

## Main features

- Configurable server Navigator
- Offline server requests with retries and countdowns
- Crafty Controller power control and live discovery
- Pterodactyl and Generic HTTP provider support
- Always-On provider for servers HubPilot should not power-manage
- Server start/stop controls when a power provider is configured
- Idle and failed-request shutdown for resource saving
- Per-server version and compatibility rules
- Owner, Admin, Moderator, and Helper roles
- In-game admin tools and protected HubPilot items
- Entity, sign, portal, and supported mannequin bindings
- HubPilot telemetry through Link
- `/hub` and `/lobby` routing
- [ViaVersion](https://github.com/ViaVersion/ViaVersion) compatibility
- [LuckPerms](https://github.com/LuckPerms/LuckPerms) support for hub-side permission nodes

## Quick install

### 1. Velocity proxy

Put `HubPilot-Core-1.0.0.jar` in the Velocity proxy's `plugins` folder.

```text
velocity/plugins/HubPilot-Core-1.0.0.jar
```

### 2. Hub server

Put the hub-side JARs in the hub Paper/Bukkit server's `plugins` folder.

```text
hub/plugins/HubPilot-Hub-1.0.0.jar
hub/plugins/HubPilot-Interact-1.0.0.jar
hub/plugins/HubPilot-Link-1.0.0.jar
```

`HubPilot-Hub` and `HubPilot-Link` belong on the hub. `HubPilot-Interact` is optional if you do not need entity, sign, portal, or mannequin bindings.

Do not keep multiple versions of the same HubPilot component in one `plugins` folder.

### 3. Keep the hub online

The simplest resource-saving layout is one lightweight hub that stays online while larger backend servers are allowed to shut down when nobody is using them.

A common setup looks like this:

```text
Hub       -> Always-On
Survival  -> Crafty / another power provider
Modded    -> Crafty / another power provider
Minigames -> Crafty / another power provider
```

That gives players somewhere to stay while an offline server starts without forcing every backend to run all day.

### 4. Give each server its own port

Every server behind Velocity needs its own listening port.

Using one range keeps things easy to read and troubleshoot:

```text
Velocity public port: 25565

Hub:                25600
Survival:           25601
Modded Survival:    25602
Minigames:          25603
```

The `2560x` range is only a suggestion. Any unused ports will work.

If players always connect through Velocity, those backend ports normally stay internal and do not need to be exposed to the internet.

### 5. Finish setup

Restart Velocity first, then restart the hub and backend servers.

Join the hub as an operator and run:

```text
/hp claimowner
/hp setup
```

Pick the provider that matches how the servers are hosted. If it uses an API key or token, follow that provider's setup section in [Server Providers](docs/PROVIDERS.md) before adding servers.

Once the provider is connected, use `/hp discover` and the admin GUI to add and manage destinations.

The full walkthrough is in [Installation](docs/INSTALLATION.md).

## Provider options

### Crafty Controller

Use Crafty when HubPilot should start and stop servers through Crafty Controller. This is the provider with the most real-network testing behind it.

### Always-On

Always-On tells HubPilot to leave power management to something else.

It works well for:

- the hub server
- servers that stay online 24/7
- paid hosts that handle startup themselves
- Docker or systemd servers managed outside HubPilot
- hosts without an API HubPilot can use

Always-On does not need a controller URL or API key. If the server is offline, HubPilot cannot start it.

### Pterodactyl / Generic HTTP

Both are included for other hosting setups. They have controlled test coverage, but they have not had the same live beta testing as Crafty yet.

See [Server Providers](docs/PROVIDERS.md) for API-key and endpoint setup.

## ViaVersion

HubPilot works with [ViaVersion](https://github.com/ViaVersion/ViaVersion).

If the installed Velocity/ViaVersion setup can connect a player to a backend, HubPilot can request, start, and route to that backend unless a strict HubPilot version rule blocks it.

Modded servers can still require the right Minecraft version, loader, or modpack even when ViaVersion supports the protocol.

## LuckPerms

HubPilot works with [LuckPerms](https://github.com/LuckPerms/LuckPerms) on the Paper/Bukkit hub.

LuckPerms can grant normal HubPilot permission nodes such as `hubpilot.admin`, `hubpilot.navigator`, and the Interact permissions. It is optional because HubPilot also has its own Owner, Admin, Moderator, and Helper roles.

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
- `/hpi` or `/hubpilotinteract` - Interact tools
- `/hpl` or `/hubpilotlink` - Link information
- `/hub` and `/lobby` - return to the hub

See [Commands](docs/COMMANDS.md) for the full list.

## Compatibility and testing

- Core runs on Velocity.
- Hub, Interact, and Link target the Bukkit/Paper 1.21 API family.
- Release-candidate field testing used Velocity 4.1.0-SNAPSHOT build 16.
- Crafty Controller is the only external controller with live beta coverage for 1.0.0.
- HubPilot works alongside ViaVersion when the installed ViaVersion setup supports the connection.
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

HubPilot was built on top of AutoServer by Artificial-720. The original AutoServer MIT notice is kept in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and inside the release JARs.
