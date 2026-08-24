# Changelog

## 1.0.1 - 2026-08-24

The first post-release update adds a per-server **Always-On server** option and live Navigator telemetry refresh without changing the existing provider system.

### Added

- Always-On server toggle in Hub Automation Settings
- global default and per-server override support through `hubpilot.properties`
- Core handling for the new setting over the existing `hubpilot:settings` sync path
- live Navigator telemetry updates while the GUI stays open

When Always-On server is enabled on a managed server, HubPilot keeps provider startup available but disables its automatic idle, failed-request, and queue-empty shutdown behavior for that server. Manual Stop Server behavior is unchanged.

Open Navigator menus now refresh their server items after each complete status sync. The refresh uses the existing status cycle and happens once at `STATUS_SYNC_END`, so it does not add another repeating scheduler or rebuild once per server row.

Core and Hub should be updated together for 1.0.1. Interact 1.0.0 and Link 1.0.0 remain compatible because they have no functional changes in this release. Matching 1.0.1 builds are still published with the suite.

## 1.0.0 - 2026-08-23

HubPilot 1.0.0 is the first public release of the full suite: Core, Hub, Interact, and Link.

### Included

- Velocity routing and server requests
- Player Navigator and in-game admin tools
- First-run owner/setup flow
- Owner, Admin, Moderator, and Helper roles
- Always-On provider
- Crafty Controller integration
- Pterodactyl and Generic HTTP providers
- Provider-backed start and stop controls
- Server discovery and Navigator import
- Retry and countdown handling for offline requests
- Idle and failed-request shutdown for resource saving
- Server status and telemetry
- Entity, sign, portal, and supported mannequin bindings
- `/hub` and `/lobby` routing
- ViaVersion compatibility
- LuckPerms support for hub-side permission nodes
- Full provider setup docs and troubleshooting FAQ

### Testing

Crafty Controller is the only external controller that had live beta coverage for this release. Pterodactyl and Generic HTTP are included, but they have only been through controlled testing so far.

ViaVersion support follows the ViaVersion setup installed on the proxy. If Velocity/ViaVersion can connect the player to a backend, HubPilot can route there unless a strict HubPilot version rule blocks it.

### 3.5.18 fixes carried into 1.0.0

- Stop Server resolves the selected destination to the real Velocity backend before Core looks up the managed server/provider.
- Crafty discovery checks Velocity candidates against a fresh Crafty server list.
- Crafty provider-ID repair uses the live Crafty inventory instead of stale filesystem/config/script data.
- Existing managed-server config is not deleted just because one Crafty inventory response no longer contains that server.
