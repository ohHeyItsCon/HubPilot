# Changelog

## 1.0.2 - 2026-09-01 (pre-release)

The **Queue Update** adds configurable join-flow messages and clearer startup-queue feedback.

### Added

- Interact brush with binding, portal selection, inspection, removal and label-height modes
- Nether, End, water and invisible portal particle styles
- automatic floating destination labels for existing and new bindings
- /hpi items toggles with persistent admin-item hiding and inventory safeguards


- in-game Join & Queue Messages editor under Automation Settings
- per-event show/hide controls, text editing, previews, and reset/inherit controls
- global message defaults with optional per-server overrides
- queue position when joining and updates only when the position changes
- separate configurable `/hp cancel` success and no-pending-request messages
- `{server}` formatting that matches the Navigator server-name style
- event placeholders for queue, countdown, retry, version, and failure information
- backward compatibility with flat 1.0.1 message files and `<placeholder>` syntax

### Fixed

- September 3 Core repair: startup/reload derives a protected hub role for existing managed entries after all configuration inheritance; idle, failed-request, and queue-empty shutdown are disabled, with a final guard for pre-reload sessions. Original files and startup preferences are preserved. That change rebuilt only Core; see the supplied checksums to distinguish it from the original 1.0.2 artifact.
- hiding countdown text no longer disables the configured countdown sound
- Paper countdown feedback now preserves custom message colors instead of forcing yellow
- the trusted settings filter now carries valid message settings without accepting unrelated or sensitive keys
- `/hp discover` now protects the configured hub across listing, suggestions, bulk discovery, and final import, including common equivalent hub names
- the configured hub is now exempt from automatic idle shutdown even if discovery created a managed entry for it or it inherits a global idle timeout

For the latest additions, update Hub and Interact together. Keep the September 3 Core build installed; Link remains unchanged.

See [release/1.0.2/RELEASE-NOTES.md](release/1.0.2/RELEASE-NOTES.md) for update instructions and current field-test status.

## 1.0.1 - 2026-08-24

The first post-release update adds a per-server **Always-On server** option and live Navigator telemetry refresh without changing the existing provider system.

### Added

- Always-On server toggle in Hub Automation Settings
- global default and per-server override support through `hubpilot.properties`
- Core handling for the new setting over the existing `hubpilot:settings` sync path
- live Navigator telemetry updates while the GUI stays open

### Fixed during testing

- Crafty-only servers can now be discovered from Crafty's API-reported name, address, port, and UUID without editing `velocity.toml`.
- Crafty-discovered Velocity registrations are persisted and restored when Core restarts.
- Crafty discovery now honors trusted-LAN TLS mode consistently when the panel is addressed by a LAN IP.
- Crafty display names are reconciled with equivalent existing Velocity names so discovery does not create hyphenated duplicates.
- Crafty servers are also reconciled by their registered Velocity address, covering aliases such as `the-hub` and `hub` whose names are not textually equivalent.
- Discovery repairs aliases imported by earlier prerelease builds: duplicate Core definitions are archived for recovery and only their matching Navigator destinations are removed.
- The Admin destination menu and player Navigator now render destinations from the same saved slot layout, and admin clicks resolve through that shared layout.
- Admin destination cards retain their exact server identity in the open inventory, restoring left-click editing after the shared-layout change.
- Fixed a Paper runtime linkage error in the Admin click patch by compiling `Player.openInventory` with Bukkit's correct `InventoryView` return type.

Full release notes, including which fixes introduced follow-up bugs and how each was corrected, are available in [release/1.0.1/RELEASE-NOTES.md](release/1.0.1/RELEASE-NOTES.md).

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


