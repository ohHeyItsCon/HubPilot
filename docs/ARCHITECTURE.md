# Architecture

HubPilot is split into four plugins because the Velocity proxy and the hub server have different jobs.

## Core

Core runs on Velocity. It handles network-wide routing, server requests, provider selection, discovery, owner/staff state, status, statistics, and the main `/hp` command.

## Hub

Hub runs on the Paper/Bukkit hub server. It handles the Navigator, admin inventories, protected HubPilot items, setup screens, and hub-side authorization.

## Interact

Interact also runs on the hub. It connects world objects to HubPilot destinations through entities, signs, portals, and supported native mannequins.

## Link

Link runs beside Hub on the Paper/Bukkit hub server. It carries the HubPilot communication and telemetry the hub needs from Core.

That telemetry stays inside the HubPilot network. Link does not send it to an outside analytics or tracking service.

Backend game servers do not need HubPilot JARs for normal routing, server requests, or provider power control.

## Versioning and component compatibility

HubPilot uses one release number across Core, Hub, Interact, and Link. That keeps the suite easy to track and gives major internal changes a clean way to require matching versions.

Core and Hub should run the exact same HubPilot version.

Link and Interact can stay on an older build when a release does not change them and the release notes say the older build is still compatible. They should not be more than **two published HubPilot releases behind** Core and Hub.

If a release explicitly requires a new Link or Interact build, update it even if it is still inside that normal two-release window.

## Recommended network layout

A simple HubPilot network keeps a lightweight hub online and lets larger backend servers start only when they are needed.

A typical setup looks like this:

- Hub uses the Always-On provider.
- Backend servers use Crafty or another power provider when on-demand startup is wanted.
- Every server behind Velocity has its own internal port.

Using ports such as `25600`, `25601`, and `25602` keeps things easy to follow, but that range is not required.

## Power providers

Each managed server can have its own provider.

- `always-online` leaves power management to something outside HubPilot.
- Crafty, Pterodactyl, and Generic HTTP can send real power requests.

Provider credentials stay in Core. Hub, Interact, and Link do not need them.

When Stop Server is used, HubPilot resolves the selected Navigator or destination entry to the real Velocity backend before it looks up the provider target. That keeps a display name or alias from being mistaken for the controller's server ID.

## ViaVersion

HubPilot works alongside [ViaVersion](https://github.com/ViaVersion/ViaVersion).

ViaVersion handles protocol translation. HubPilot handles the request, startup flow, and destination routing. If the installed ViaVersion setup can connect the player to a backend, HubPilot can route there unless a strict HubPilot version rule blocks it.

## Testing

Crafty Controller is the only external controller with live beta coverage so far. Pterodactyl and Generic HTTP are implemented and have controlled test coverage, but they have not had the same live network testing yet.

See [Project Transparency](../TRANSPARENCY.md) for more detail.
