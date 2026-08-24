# Architecture

HubPilot is split into four plugins because the proxy and hub server have different jobs.

## Core

Core runs on Velocity and handles network-wide routing, server requests, provider selection, discovery, owner/staff state, status, statistics, and the main `/hp` command.

## Hub

Hub runs on the hub's Bukkit/Paper server. It handles the Navigator, admin inventories, protected HubPilot items, setup screens, and the hub side of authorization.

## Interact

Interact also runs on the hub. It connects world objects to HubPilot destinations through entities, signs, portals, and supported native mannequins.

## Link

Link runs on the hub Bukkit/Paper server alongside Hub. It provides the HubPilot link/telemetry bridge used by the hub.

Backend game servers do not need HubPilot JARs for normal routing, server requests, or provider power control.

## Versioning and component compatibility

HubPilot releases use one unified version number across Core, Hub, Interact, and Link. This keeps the suite on one compatibility line and makes it possible for a release to require matching components when shared protocols or internal formats change.

Core and Hub should run the exact same HubPilot version.

Link and Interact can remain on an older build when a release does not change those components and the release notes state that the existing build remains compatible. They should not be more than **two published HubPilot releases behind** Core and Hub.

If a release explicitly requires a new Link or Interact build, that requirement overrides the two-release grace period and the affected component should be updated immediately.

## Recommended network layout

HubPilot works best with a small hub server that stays online while larger backend servers are started only when they are needed.

A typical setup is:

- Hub uses the Always-On provider.
- Backend servers use Crafty or another power provider when on-demand startup is wanted.
- Each server behind Velocity has its own internal port.

A `2560x` port layout such as `25600`, `25601`, and `25602` is easy to manage, but the range itself is not required.

## Power providers

Each managed server can use a provider.

- `always-online` leaves server power management to another system.
- Crafty, Pterodactyl, and Generic HTTP can send actual power requests.

Provider credentials stay in Core and are not needed by Hub, Interact, or Link.

For Stop Server, the 3.5.18 release candidate resolves the selected Navigator/destination entry to the real Velocity backend target before Core resolves the managed-server/provider entry. This prevents a display or destination alias from being used as the controller's server identity.

## ViaVersion

HubPilot works alongside [ViaVersion](https://github.com/ViaVersion/ViaVersion).

Protocol translation stays with ViaVersion. HubPilot handles the request, startup flow, and destination routing. If the installed ViaVersion setup can connect the player to a backend, HubPilot can route to that backend unless a strict version rule is configured in HubPilot.

## Testing status

Crafty Controller is the only external controller integration covered by live beta testing so far. Pterodactyl and Generic HTTP are implemented and have controlled test coverage, but not live beta coverage yet.

See [Project Transparency](../TRANSPARENCY.md).
