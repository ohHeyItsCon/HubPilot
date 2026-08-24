# Frequently Asked Questions

## Why isn't my server starting with Crafty, Pterodactyl, Generic HTTP, or another controller setup?

Check the pieces that connect Velocity, HubPilot, and the controller before assuming the plugin itself is broken.

Start with the **server name**. The Velocity server entry, the HubPilot destination/server entry, and the controller mapping all need to lead to the same backend. Display names can be different, but the real backend/provider mapping still has to match.

Check the **port** next. Every Minecraft server behind Velocity needs its own listening port. `25600`, `25601`, `25602`, and so on are easy to manage, but any unused ports work. Velocity has to point to the port Minecraft is actually listening on.

Check the **Velocity address**. `127.0.0.1` only works when Velocity can reach that backend through localhost. Docker containers, NAS containers, separate machines, and some hosting setups need a LAN address, container hostname, or another internal address.

Check the **provider**. A server using `always-online` will not be started by HubPilot because another system owns that process.

Check `startup.provider-server-id`. Crafty needs the Crafty UUID, Pterodactyl needs the Pterodactyl server identifier, and Generic HTTP needs whatever ID the remote API expects.

Check the **controller mapping** too. Renaming, recreating, or deleting a server in the panel can leave an old mapping behind.

Then check the **controller connection**. Make sure the URL, API key/token, permissions, and TLS settings are correct.

Finally, check the **startup timeout**. Large modded servers can take much longer to become reachable than a small Paper server. If the controller starts the process but HubPilot gives up before Minecraft starts listening, raise the startup or ping timeout for that server.

Provider setup is covered in [Server Providers](PROVIDERS.md).

For Crafty, `/hp discover` is also a quick way to see whether HubPilot currently sees the server as a valid Velocity/Crafty candidate.

## My server starts, but HubPilot never sends me to it. What should I check?

Make sure Minecraft is actually accepting connections on the address and port registered in Velocity.

A controller showing **Running** only means the process is running. It does not mean the Minecraft server is ready for players yet.

Also check:

- Velocity backend address and port
- HubPilot destination target
- startup and ping timeout values
- client/server Minecraft compatibility
- required mod loader or modpack
- Velocity forwarding required by that backend

If HubPilot retries several times and then shuts the server down, the backend probably never became connectable before the retry window ended.

## Does HubPilot support ViaVersion?

Yes. HubPilot works with [ViaVersion](https://github.com/ViaVersion/ViaVersion).

If the installed ViaVersion setup lets a client connect to a backend through Velocity, HubPilot can request, start, and route to that server.

Strict HubPilot version rules can still require an exact version when a server should not allow cross-version connections. Modded servers may also need the correct loader or modpack even when ViaVersion supports the underlying protocol.

ViaVersion handles protocol translation. HubPilot handles the request and routing.

## Does HubPilot work with LuckPerms?

Yes. HubPilot works with [LuckPerms](https://github.com/LuckPerms/LuckPerms) on the Paper/Bukkit hub.

LuckPerms can grant HubPilot permission nodes to users and groups. It is optional because HubPilot also has its own Owner, Admin, Moderator, and Helper roles.

HubPilot Owner is still managed by HubPilot itself and is not automatically granted by a LuckPerms group.

See [Permissions and Roles](PERMISSIONS.md) for the available nodes.

## Do all backend servers need HubPilot installed?

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

Backend game servers normally do not need HubPilot JARs for requests, startup/shutdown, or routing through Velocity.

## Do I need to update every HubPilot component with every release?

Not always.

Core and Hub should stay on the exact same HubPilot version and should normally be updated together.

Link and Interact can stay on an older build when the release notes say that build is still compatible. They should not be more than **two published HubPilot releases behind** Core and Hub.

If a release says Link or Interact must be updated, update it even if it is still inside the normal two-release window.

HubPilot still publishes one suite version so it is clear which builds belong to the same release.

## Does the hub server have to stay online?

It is strongly recommended for the normal resource-saving layout.

A small always-online hub gives players somewhere to wait while larger game servers start only when somebody asks for them.

```text
Hub       -> Always-On
Survival  -> Crafty / another power provider
Modded    -> Crafty / another power provider
Minigames -> Crafty / another power provider
```

Large networks with enough hardware can keep more servers online if they want. Future operating-mode presets are listed in the [roadmap](ROADMAP.md).

## What does the Always-On provider actually do?

It tells HubPilot not to control that server's power.

Use it for:

- the hub
- 24/7 servers
- paid hosts that handle startup
- manually managed servers
- Docker or systemd setups controlled outside HubPilot
- hosts without a usable API

Always-On does not need a controller URL or API key.

If an Always-On server is offline, something outside HubPilot has to start it.

## Why doesn't `/hp discover` show one of my servers?

First, make sure the server is registered in Velocity.

When Crafty is the primary provider, HubPilot also checks the current Crafty inventory. The server needs to exist in Crafty and be a valid Velocity candidate.

Check:

- the server still exists in Crafty
- the Velocity entry points to the right backend
- the backend name/mapping is correct
- the Crafty API connection works
- the server was not recreated with a new Crafty UUID

An old Velocity entry is not treated as proof that a Crafty server still exists.

## Do backend servers have to use ports in the `25600` range?

No. That is only a suggested convention.

Every backend simply needs its own unique port.

```text
Velocity public port: 25565

Hub:                25600
Survival:           25601
Modded Survival:    25602
Minigames:          25603
```

A predictable range just makes setup and troubleshooting easier.

If players always connect through Velocity, backend ports normally do not need to be exposed directly to the internet.

## Can HubPilot manage servers on another machine?

Yes, as long as Velocity can reach the Minecraft backend and the configured provider can control it.

The backend does not need to be on the same machine as Velocity.

The address might be:

- a LAN IP
- a Docker/container hostname
- a private network address
- another reachable hostname

Do not use `127.0.0.1` for a backend on another machine or an isolated container network.

## What happens when nobody is using a server?

Provider-controlled servers can shut down according to their configured idle settings.

HubPilot also supports failed-request shutdown. If a server was started for a player but every connection attempt fails, HubPilot can stop it again instead of leaving an unused server running.

Both behaviors are optional.

## Can HubPilot be used without Crafty Controller?

Yes.

HubPilot includes:

- Always-On
- Crafty Controller
- Pterodactyl
- Generic HTTP

Crafty is simply the only external controller with live beta coverage so far. Pterodactyl and Generic HTTP have controlled testing behind them.

## Can HubPilot work with modded Minecraft servers?

Yes, as long as the backend is registered behind Velocity and the client can actually connect to it.

A modded backend may require:

- an exact Minecraft version
- the correct loader
- the same modpack
- the forwarding setup required by that server

Use HubPilot strict-version rules when incompatible clients should be blocked before a transfer is attempted.

ViaVersion can still be used where cross-version clients are actually compatible.

## Why did HubPilot stop my server after I tried to join it?

The server may have been started for that request, but the connection never completed.

If failed-request shutdown is enabled, HubPilot can stop the server again after the configured retries fail, as long as it is safe to do so.

That keeps a failed join from leaving an empty server running forever.

Check the backend log and the HubPilot/Velocity logs to find out why the connection failed before turning that feature off.
