# Installation

## 1. Velocity proxy

Put `HubPilot-Core-1.0.0.jar` in the Velocity proxy's `plugins` folder.

```text
velocity/plugins/HubPilot-Core-1.0.0.jar
```

Core belongs on Velocity only.

## 2. Hub Paper/Bukkit server

Put the hub-side JARs in the **hub server's** `plugins` folder.

```text
hub/plugins/HubPilot-Hub-1.0.0.jar
hub/plugins/HubPilot-Interact-1.0.0.jar
hub/plugins/HubPilot-Link-1.0.0.jar
```

- `HubPilot-Hub` is required for the HubPilot hub UI and setup flow.
- `HubPilot-Link` belongs on the hub server alongside Hub.
- `HubPilot-Interact` is optional if entity, sign, portal, or mannequin bindings are not needed.

Do not keep multiple versions of the same HubPilot component in one `plugins` folder.

Backend game servers do not need HubPilot JARs for normal routing, server requests, or provider power control.

## 3. Use an always-on hub

A small hub server that stays online is the recommended HubPilot layout.

The hub gives players a permanent landing point while larger game servers can stay offline until somebody requests them. This is where the resource-saving setup works best.

A common layout is:

```text
Hub       -> Always-On
Survival  -> Crafty / another power provider
Modded    -> Crafty / another power provider
Minigames -> Crafty / another power provider
```

## 4. Give each server behind Velocity its own port

Every hub and backend server registered behind Velocity needs its own listening port.

A simple convention is to use the `2560x` range:

```text
Velocity public port: 25565

Hub:                25600
Survival:           25601
Modded Survival:    25602
Minigames:          25603
```

The `2560x` range is only an example. Any unused ports will work.

Example Velocity server entries:

```toml
[servers]
hub = "127.0.0.1:25600"
survival = "127.0.0.1:25601"
modded = "127.0.0.1:25602"
try = ["hub"]
```

`127.0.0.1` only works when Velocity can actually reach the backend through localhost. If Velocity and the server are on separate machines or isolated container networks, use the correct LAN address, container hostname, or another reachable internal address.

If Velocity is the public entry point, backend ports normally stay internal and do not need to be exposed directly to players.

## 5. Restart

Restart Velocity first, then restart the hub and backend servers.

## 6. Claim the installation

Join the configured hub as an operator and run:

```text
/hp claimowner
```

HubPilot Owner is explicit and UUID-based. OP can provide an admin fallback, but OP alone does not create the Owner.

## 7. Run setup and configure the provider

```text
/hp setup
```

Choose the provider that matches how the servers are hosted.

- **Crafty Controller:** follow [Crafty Controller setup](PROVIDERS.md#crafty-controller-setup).
- **Pterodactyl:** follow [Pterodactyl setup](PROVIDERS.md#pterodactyl-setup).
- **Generic HTTP:** follow [Generic HTTP setup](PROVIDERS.md#generic-http-setup).
- **Always-On:** no API key is required. Use it when another system keeps the server running.

The provider guide explains how to create or obtain the API credential, what URL HubPilot expects, how server IDs are mapped, and what goes into `providers.yml` and `secrets.yml` if setup is done manually.

Do not add an API path to a Crafty or Pterodactyl panel URL unless the provider guide specifically says to. HubPilot adds the API path for those two providers itself.

## 8. Test the provider, then discover backends

Once the provider URL and credential are saved, use the provider test in HubPilot before adding servers.

For Crafty, run:

```text
/hp discover
```

Add one with:

```text
/hp discover add <server>
```

When Crafty is the primary provider, discovery checks Velocity candidates against a fresh Crafty server list. Old Velocity registrations for servers deleted from Crafty are filtered out.

For providers that do not have Crafty-style discovery, make sure the server's `startup.provider` and `startup.provider-server-id` point to the correct provider and controller server ID.

## 9. ViaVersion

HubPilot is compatible with [ViaVersion](https://github.com/ViaVersion/ViaVersion).

HubPilot does not replace ViaVersion's protocol translation. If the installed ViaVersion setup can connect a client to a backend, HubPilot can request and route to that backend unless a strict HubPilot version rule blocks it.

A modded server can still require the correct Minecraft version, loader, or modpack.

## 10. LuckPerms

HubPilot is compatible with [LuckPerms](https://github.com/LuckPerms/LuckPerms) on the Paper/Bukkit hub.

LuckPerms is optional. HubPilot has its own role system, but LuckPerms can grant the HubPilot permission nodes listed in [Permissions and Roles](PERMISSIONS.md).

HubPilot Owner is still claimed and stored by HubPilot itself.

## 11. Open the admin GUI

```text
/hp gui
```

Use the GUI for normal in-game server and Navigator management after setup.

If a server will not start or a player cannot connect after startup, check the [FAQ and troubleshooting guide](FAQ.md).
