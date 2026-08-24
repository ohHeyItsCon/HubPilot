# Installation

## 1. Velocity proxy

Put `HubPilot-Core-1.0.1.jar` in the Velocity proxy's `plugins` folder.

```text
velocity/plugins/HubPilot-Core-1.0.1.jar
```

Core only belongs on Velocity.

## 2. Hub Paper/Bukkit server

Put the hub-side JARs in the hub server's `plugins` folder.

```text
hub/plugins/HubPilot-Hub-1.0.1.jar
hub/plugins/HubPilot-Interact-1.0.1.jar
hub/plugins/HubPilot-Link-1.0.1.jar
```

- `HubPilot-Hub` is required for the HubPilot hub UI and setup flow.
- `HubPilot-Link` belongs on the hub beside Hub.
- `HubPilot-Interact` is optional if you do not need entity, sign, portal, or mannequin bindings.

Do not keep multiple versions of the same HubPilot component in one `plugins` folder.

Backend game servers do not need HubPilot JARs for normal routing, server requests, or provider power control.

### Updating from 1.0.0

Update Core and Hub together to 1.0.1.

Link 1.0.0 and Interact 1.0.0 remain compatible with 1.0.1 because they have no functional changes in this release. Matching 1.0.1 builds are still included so new installs can keep the whole suite on one version number.

## 3. Keep a hub online

The easiest HubPilot layout is one small hub that stays online while larger game servers are allowed to shut down when nobody is using them.

That gives players a permanent place to land while an offline backend starts.

A common layout:

```text
Hub       -> Always-On
Survival  -> Crafty / another power provider
Modded    -> Crafty / another power provider
Minigames -> Crafty / another power provider
```

Starting in 1.0.1, a provider-controlled server that should stay running can also use the **Always-On server** toggle in Hub Automation Settings. This is different from the Always-On provider. The provider still controls startup and manual stop, while the lifecycle toggle prevents HubPilot from automatically shutting that server down.

## 4. Give every server its own port

Every hub and backend registered behind Velocity needs its own listening port.

A simple convention is the `2560x` range:

```text
Velocity public port: 25565

Hub:                25600
Survival:           25601
Modded Survival:    25602
Minigames:          25603
```

That range is only an example. Any unused ports will work.

Example Velocity entries:

```toml
[servers]
hub = "127.0.0.1:25600"
survival = "127.0.0.1:25601"
modded = "127.0.0.1:25602"
try = ["hub"]
```

`127.0.0.1` only works when Velocity can actually reach the backend through localhost. Separate machines and isolated container networks usually need a LAN address, container hostname, or another reachable internal address.

If Velocity is the public entry point, backend ports normally stay internal and do not need to be exposed directly to players.

## 5. Restart

Restart Velocity first, then restart the hub and backend servers.

## 6. Claim the installation

Join the configured hub as an operator and run:

```text
/hp claimowner
```

HubPilot Owner is stored by UUID. OP can be used as an admin fallback, but being OP does not make somebody the HubPilot Owner.

## 7. Run setup and connect the provider

```text
/hp setup
```

Pick the provider that matches how the servers are hosted.

- **Crafty Controller:** [Crafty setup](PROVIDERS.md#crafty-controller-setup)
- **Pterodactyl:** [Pterodactyl setup](PROVIDERS.md#pterodactyl-setup)
- **Generic HTTP:** [Generic HTTP setup](PROVIDERS.md#generic-http-setup)
- **Always-On:** no API key is needed because another system owns the process

The provider guide shows how to get the API credential, what URL HubPilot expects, how server IDs are mapped, and what the manual `providers.yml` / `secrets.yml` setup looks like.

Do not add `/api/v2` to a Crafty URL or `/api/client` to a Pterodactyl URL. HubPilot adds those paths itself.

## 8. Test the provider and add backends

Test the provider after the URL and credential are saved.

For Crafty, run:

```text
/hp discover
```

Add a server with:

```text
/hp discover add <server>
```

When Crafty is the primary provider, discovery checks Velocity candidates against a fresh Crafty server list. Old Velocity entries for servers that no longer exist in Crafty are filtered out.

For other providers, make sure `startup.provider` and `startup.provider-server-id` point to the right provider and controller server ID.

## 9. ViaVersion

HubPilot works with [ViaVersion](https://github.com/ViaVersion/ViaVersion).

HubPilot does not do protocol translation itself. If the installed ViaVersion setup can connect a client to a backend, HubPilot can request and route to it unless a strict HubPilot version rule blocks the transfer.

Modded servers can still require the right Minecraft version, loader, or modpack.

## 10. LuckPerms

HubPilot works with [LuckPerms](https://github.com/LuckPerms/LuckPerms) on the Paper/Bukkit hub.

LuckPerms is optional. HubPilot has its own roles, but LuckPerms can grant the permission nodes listed in [Permissions and Roles](PERMISSIONS.md).

HubPilot Owner is still claimed and stored by HubPilot itself.

## 11. Open the admin GUI

```text
/hp gui
```

Use the GUI for normal server and Navigator management after setup.

If a server will not start or a player never gets transferred after startup, check the [FAQ](FAQ.md).
