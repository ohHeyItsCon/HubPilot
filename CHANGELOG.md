# Changelog

## 1.0.0 - 2026-08-23

The first public HubPilot release includes Core, Hub, Interact, and Link.

### Included

- Velocity routing and server-request handling
- Player Navigator and in-game administration
- First-run owner/setup flow
- Owner, Admin, Moderator, and Helper roles
- Always-On mode
- Crafty Controller integration
- Pterodactyl and Generic HTTP provider adapters
- Provider-backed start/stop controls
- Server discovery and Navigator import
- Retry/countdown handling for offline requests
- Idle and failed-request shutdown for resource saving
- Server status and telemetry
- Entity, sign, portal, and supported mannequin bindings
- `/hub` and `/lobby` routing
- ViaVersion-compatible routing
- LuckPerms compatibility for hub-side permission nodes
- Full provider setup documentation and troubleshooting FAQ

### Testing status

Crafty Controller is the only external server-management controller covered by live beta testing for this release. Pterodactyl and Generic HTTP are included but are not yet listed as live-tested integrations.

ViaVersion compatibility follows the installed ViaVersion setup. HubPilot can request and route to backends that Velocity/ViaVersion can actually connect the player to, unless a HubPilot strict-version rule blocks that destination.

### 3.5.18 release-candidate fixes carried into 1.0.0

- Stop Server resolves a selected destination to the real backend target before Core looks up the managed server/provider.
- Crafty discovery checks Velocity candidates against a fresh Crafty server list.
- Crafty provider-ID repair uses the live Crafty inventory instead of old filesystem/config/script data.
- Existing managed-server config is not automatically deleted just because one Crafty inventory response no longer contains the server.
