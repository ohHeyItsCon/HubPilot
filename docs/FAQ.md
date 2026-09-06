# Frequently Asked Questions

## Why isn't my server starting?

Start with the mapping between Velocity, HubPilot, and the controller.

Start with the **server name**. The Velocity server entry, the HubPilot destination/server entry, and the controller mapping all need to lead to the same backend. Display names can be different, but the real backend/provider mapping still has to match.

Check the **port** next. Every Minecraft server behind Velocity needs its own listening port. `25600`, `25601`, `25602`, and so on are easy to manage, but any unused ports work. Velocity has to point to the port Minecraft is actually listening on.

Check the **Velocity address**. `127.0.0.1` only works when Velocity can reach that backend through localhost. Docker containers, NAS containers, separate machines, and some hosting setups need a LAN address, container hostname, or another internal address.

Check the **provider**. A server using `always-online` will not be started by HubPilot because another system owns that process.

Check `startup.provider-server-id`. Crafty needs the Crafty UUID, Pterodactyl needs the Pterodactyl server identifier, and Generic HTTP needs whatever ID the remote API expects.

Check the **controller mapping** too. Renaming, recreating, or deleting a server in the panel can leave an old mapping behind.

Then check the **controller connection**. Make sure the URL, API key/token, permissions, and TLS settings are correct.

Finally, check the **startup timeout**. Large modded servers can take much longer to become reachable than a small Paper server. If the controller starts the process but HubPilot gives up before Minecraft starts listening, raise the startup or ping timeout for that server.

Provider setup is covered in [Server Providers](PROVIDERS.md).

For Crafty, `/hp discover` shows the servers HubPilot can import.

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

No. Put Core on Velocity and Hub, Link, and optional Interact on the Paper/Bukkit hub. Backend game servers don't need HubPilot JARs for normal requests, power control, or routing. See [Installation](INSTALLATION.md).

## Do I need to update every HubPilot component with every release?

Update Core and Hub together so their versions match.

Link and Interact can stay on an older build when the release notes say that build is still compatible. They should not be more than **two published HubPilot releases behind** Core and Hub.

For 1.0.1 specifically, Core and Hub need the update. Link 1.0.0 and Interact 1.0.0 remain compatible because neither one has functional changes in 1.0.1.

For the latest 1.0.2 prerelease, use the newer Hub and Interact builds for the editing tools. Link 1.0.1 remains compatible. Check the [release notes](../release/1.0.2/RELEASE-NOTES.md) and supplied checksums.

## Does the hub server have to stay online?

Keep a hub online so players have somewhere to wait while other servers start. You can keep additional servers running too.

If the hub is shutting down unexpectedly, check [Known Bugs](../KNOWN_BUGS.md) for the 1.0.1 discovery problem and the 1.0.2 repair.

## What does the Always-On provider actually do?

It tells HubPilot not to control that server's power.

Use it for:

- the hub
- 24/7 servers managed outside HubPilot
- paid hosts that handle startup
- manually managed servers
- Docker or systemd setups controlled outside HubPilot
- hosts without a usable API

Always-On does not need a controller URL or API key.

If an Always-On provider server is offline, something outside HubPilot has to start it.

## What is the difference between the Always-On provider and the Always-On server option?

The **Always-On provider** means HubPilot does not own power control for that server at all. HubPilot cannot start or stop it.

The **Always-On server** option added in 1.0.1 is a lifecycle setting for servers that still use a managed provider such as Crafty, Pterodactyl, or Generic HTTP. HubPilot can start those servers and manual Stop Server still works, but automatic idle, failed-request, and queue-empty shutdowns are disabled while the option is on.

## Why doesn't `/hp discover` show one of my servers?

With Crafty as the primary provider, 1.0.1 reads the current Crafty inventory and can register missing backends with Velocity. Manual registration isn't required for each new Crafty server.

Check:

- the server still exists in Crafty
- the Velocity entry points to the right backend
- the backend name/mapping is correct
- the Crafty API connection works
- the server was not recreated with a new Crafty UUID

A leftover Velocity entry won't make a deleted Crafty server discoverable. The configured hub is intentionally excluded in the repaired 1.0.2 build.

## Do backend servers have to use ports in the `25600` range?

No. Use any unused port for each server. The `25600` range in [Installation](INSTALLATION.md) is just an example.

If players always connect through Velocity, backend ports normally do not need to be exposed directly to the internet.

## Can HubPilot manage servers on another machine?

Yes, if Velocity can reach the backend and the provider can control it. Use a reachable LAN/private address or container hostname.

Do not use `127.0.0.1` for a backend on another machine or an isolated container network.

## What happens when nobody is using a server?

Provider-controlled servers can shut down according to their configured idle settings.

HubPilot also supports failed-request shutdown. If a server was started for a player but every connection attempt fails, HubPilot can stop it again instead of leaving an unused server running.

If **Always-On server** is enabled for that server, these automatic shutdown paths are disabled.

## Can HubPilot be used without Crafty Controller?

Yes. Pterodactyl and Generic HTTP provide power control, and Always-On leaves it to another system. Crafty has live beta coverage; Pterodactyl and Generic HTTP have controlled tests. See [Server Providers](PROVIDERS.md).

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

If the server should stay running regardless of failed joins or idle time, enable **Always-On server** in its Automation Settings.

Check the backend log and the HubPilot/Velocity logs to find out why the connection failed before changing the lifecycle behavior.
