# Architecture

HubPilot has four plugins. Core runs on the proxy; the other three run on the hub.

| Plugin | Responsibilities |
| --- | --- |
| Core | Routing, requests, providers, discovery, owner/staff records, status, statistics, and `/hp` |
| Hub | Navigator and admin menus, setup, protected items, and hub-side permission checks |
| Interact | Connections between destinations and signs, entities, portals, or supported native mannequins |
| Link | Communication and telemetry needed by the hub |

Backend servers don't need HubPilot installed for normal routing or provider power control. Telemetry stays within the HubPilot network; Link doesn't send it to an outside analytics service.

## Power control

Provider credentials stay in Core. The hub-side plugins don't need them.

Each managed server can use its own provider. Crafty, Pterodactyl, and Generic HTTP send power requests. The `always-online` provider leaves power control to another system.

When staff choose Stop Server, Core resolves the selected destination to its Velocity backend before looking up the provider target. Display names and controller IDs can differ.

The **Always-On server** setting is separate from the provider. It disables automatic shutdown while keeping provider startup and manual stop available. See [Configuration](CONFIGURATION.md#always-on-server-option).

## Routing

Velocity must be able to reach every backend at its registered address and port. [Installation](INSTALLATION.md) includes examples for localhost and separate machines or containers.

ViaVersion handles protocol translation. HubPilot requests and routes to the destination, subject to its own strict version rules.

## Component versions

Core and Hub need matching versions. Older Link and Interact builds can be used only when the release notes allow them, and at most two published releases behind. An explicit update requirement takes priority.

See [Project Transparency](../TRANSPARENCY.md) for testing coverage and release records.
