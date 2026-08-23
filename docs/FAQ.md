# Frequently Asked Questions

## Why isn't my server starting with Crafty, Pterodactyl, Generic HTTP, or another controller setup?

Before assuming HubPilot itself is failing, check the parts of the setup that connect Velocity, HubPilot, and the controller.

Start with the **server name**. The server registered in Velocity, the HubPilot destination/server entry, and the server mapped to the controller all need to point to the same backend. Display names can be different, but the backend and provider mapping still need to be correct.

Check the **server port** next. Every Minecraft server behind Velocity needs its own listening port. A simple sequence such as `25600`, `25601`, `25602`, and so on is easier to manage, but any unused ports will work. Make sure Velocity points to the port the Minecraft server is actually listening on.

Check the **address used by Velocity**. `127.0.0.1` only works when Velocity can reach that server through localhost. Docker containers, separate machines, NAS containers, and some hosting setups may need a LAN address, container hostname, or another internal address instead.

Look at the **provider assigned to the server**. A server using `always-online` will not be started by HubPilot. Always-On means another system is responsible for that process.

Check `startup.provider-server-id`. Crafty needs the Crafty server UUID, Pterodactyl needs the Pterodactyl server identifier, and Generic HTTP needs whatever server ID the remote API expects.

Check the **controller mapping**. Renaming, recreating, or deleting a server in the controller can leave an old mapping behind.

Make sure the **controller connection** is valid. Check the controller URL, API key or token, permissions, and TLS settings. HubPilot cannot start a server if the provider cannot authenticate or cannot reach the controller.

Finally, check the **startup timeout**. Large modded servers can take much longer to become ready than a small Paper server. If the controller starts the process but HubPilot gives up before Minecraft begins listening, increase the startup or ping timeout values for that server.

The API setup for each provider is covered in [Server Providers](PROVIDERS.md).

For Crafty installations, `/hp discover` can also help confirm that HubPilot currently sees the server as a valid Velocity/Crafty candidate.

## My server starts, but HubPilot never sends me to it. What should I check?

Make sure the Minecraft server is actually accepting connections on the address and port registered in Velocity.

A controller showing **Running** does not mean Minecraft is ready for players. Large modpacks may need a while after the Java process starts before the Minecraft server begins listening.

Also check:

- the Velocity backend address and port
- the HubPilot destination target
- startup and ping timeout values
- client/server Minecraft compatibility
- whether the backend requires a specific mod loader or modpack
- the Velocity forwarding setup required by that backend

If HubPilot retries several times and then shuts the server down, the backend may not have become connectable before the retry period ended.

## Does HubPilot support ViaVersion?

Yes. HubPilot is compatible with [ViaVersion](https://github.com/ViaVersion/ViaVersion) and does not replace ViaVersion's protocol translation.

If the installed ViaVersion setup allows a client to connect to a backend behind Velocity, HubPilot can still request that server, start it when a power provider is configured, and route the player to it.

HubPilot can also apply strict version rules to servers that should not allow cross-version connections. This is useful for modded servers where the correct Minecraft version, loader, or modpack matters.

ViaVersion decides protocol compatibility. HubPilot handles the server request, startup flow, and destination routing.

## Does HubPilot work with LuckPerms?

Yes. HubPilot is compatible with [LuckPerms](https://github.com/LuckPerms/LuckPerms) on the Paper/Bukkit hub.

LuckPerms can grant HubPilot permission nodes to users and groups. HubPilot also keeps its own role system, so LuckPerms is optional and does not have to be installed for HubPilot staff roles to work.

HubPilot Owner is still managed by HubPilot itself and is not automatically granted by a LuckPerms group.

See [Permissions and Roles](PERMISSIONS.md) for the available nodes.

## Do all of my backend servers need HubPilot plugins installed?

No.

The normal layout is:

```text
Velocity proxy/plugins/
└── HubPilot-Core-1.0.0.jar

Hub Paper/Bukkit server/plugins/
├── HubPilot-Hub-1.0.0.jar
├── HubPilot-Interact-1.0.0.jar
└── HubPilot-Link-1.0.0.jar
```

Backend game servers normally do not need HubPilot JARs for server requests, startup/shutdown, or routing through Velocity.

## Does the hub server have to remain online?

It is strongly recommended.

HubPilot works best with one small hub server that remains online while larger game servers start only when somebody requests them.

A common setup is:

```text
Hub       -> Always-On
Survival  -> Crafty / another power provider
Modded    -> Crafty / another power provider
Minigames -> Crafty / another power provider
```

This gives players somewhere to stay while a requested server starts and allows the larger servers to shut down when nobody is using them.

The hub can normally be kept much lighter than the actual game servers.

## What exactly does the Always-On provider do?

Always-On tells HubPilot not to manage that server's power.

Use it for:

- the hub
- servers that run 24/7
- paid hosts where the host handles startup
- servers managed manually
- Docker or systemd setups controlled outside HubPilot
- hosts that do not provide a usable API

Always-On does not require a controller URL or API key.

If an Always-On server is offline, HubPilot cannot start it. Another system must bring the server online first.

## Why doesn't `/hp discover` show one of my servers?

Start by making sure the server is registered in Velocity.

When Crafty is the primary provider, HubPilot also checks the current Crafty server inventory. The server needs to exist in Crafty and be a valid Velocity candidate.

Check:

- the server still exists in Crafty
- the Velocity server entry points to the correct backend
- the backend name and mapping are correct
- the Crafty API connection is working
- the server was not recreated in Crafty with a new controller ID

HubPilot does not treat an old Velocity entry as proof that a Crafty server still exists.

## Do my backend servers have to use ports in the `25600` range?

No. `25600`, `25601`, `25602`, and so on are only a suggested convention.

Every backend behind Velocity simply needs its own unique port.

For example:

```text
Velocity public port: 25565

Hub:                25600
Survival:           25601
Modded Survival:    25602
Minigames:          25603
```

Using one predictable range makes the setup easier to read and troubleshoot.

If players always connect through Velocity, backend ports normally do not need to be exposed directly to the internet.

## Can HubPilot manage servers on another machine?

Yes, as long as Velocity can reach the Minecraft backend and the configured provider can control it.

The Minecraft server does not need to run on the same machine as Velocity.

Depending on the setup, the Velocity backend address may be:

- a LAN IP
- a Docker/container hostname
- a private network address
- another reachable hostname

Do not use `127.0.0.1` for a server on another machine or an isolated container network.

## What happens when nobody is using a server?

For provider-controlled servers, HubPilot can shut down unused servers according to the configured idle settings.

HubPilot also supports failed-request shutdown. If a server was started for a player but all connection attempts fail, HubPilot can shut it back down instead of leaving an unused server running.

Both features are optional and are mainly meant to avoid wasting CPU and memory.

## Can HubPilot be used without Crafty Controller?

Yes.

Crafty Controller is the only external controller integration currently covered by live beta testing, but it is not required.

HubPilot includes:

- Always-On for servers whose power is managed elsewhere
- Crafty Controller
- Pterodactyl
- Generic HTTP for custom APIs, webhooks, and other hosting setups

Pterodactyl and Generic HTTP have controlled testing behind them but do not yet have the same live beta coverage as Crafty.

## Can HubPilot work with modded Minecraft servers?

Yes, as long as the backend is registered behind Velocity and the client can actually connect to it.

The important part is client compatibility. A modded backend may require:

- the exact Minecraft version
- the correct loader
- the same modpack
- the forwarding setup required by that server

HubPilot strict-version settings can be used when a server should reject incompatible clients before attempting the transfer.

ViaVersion can still be used where cross-version clients are actually compatible with the backend.

## Why did HubPilot stop my server after I tried to join it?

The server may have been started specifically for the request, but the connection never completed.

If failed-request shutdown is enabled, HubPilot can stop that server again after the configured retries fail, provided it is safe to do so.

This prevents a failed join from leaving a server running indefinitely with nobody connected.

Check the backend server log and the HubPilot/Velocity logs to find out why the connection failed before disabling failed-request shutdown.
