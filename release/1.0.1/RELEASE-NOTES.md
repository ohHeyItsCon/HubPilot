# HubPilot 1.0.1

HubPilot 1.0.1 is the first update following the public 1.0.0 release.

This release adds:

- Always-On support for managed servers
- Live Navigator telemetry
- Automatic Crafty server discovery
- A shared layout for the Admin menu and player Navigator
- Fixes for issues found during pre-release testing

## New features

### Always-On servers

Managed servers can now be marked Always-On without giving up provider control.

- Provider startup remains available.
- Staff can still stop the server manually.
- Idle, failed-request, and queue-empty shutdowns are disabled for that server.
- Crafty Controller, Pterodactyl, Generic HTTP, and other managed providers remain supported.

### Live Navigator telemetry

Open Navigator menus now refresh through HubPilot's existing status cycle.

- Server status
- Player count
- Ping
- Startup progress
- Queue information
- Uptime
- Other server-card telemetry

No additional repeating scheduler was added.

### Automatic Crafty discovery

Crafty can now act as the source of truth for discovery.

- Server name, address, port, and UUID are read from Crafty.
- Missing servers are registered with Velocity at runtime.
- Dynamic registrations are restored after Core restarts.
- New Crafty servers can be imported without editing `velocity.toml`.

### Shared Navigator layout

- The Admin destination menu and player Navigator use the same saved slot layout.
- Layout changes apply to both menus.
- Admin control slots are protected from server assignments.

## Bugs found during testing

### Crafty discovery failed when using a LAN IP

**Introduced by:** The original Crafty discovery implementation used a different HTTPS path from normal provider actions.

**Fixed by:** Applying trusted LAN certificate and hostname handling to discovery when `allow-insecure-tls` is enabled. Normal certificate verification remains unchanged when it is disabled.

### New Crafty servers were not discovered automatically

**Introduced by:** Discovery originally used Velocity's configured servers as its source and treated Crafty as verification only.

**Fixed by:** Reading Crafty's inventory directly, registering missing Velocity servers at runtime, and saving those registrations for restart restoration.

### Automatic discovery created duplicate servers

**Introduced by:** The first automatic Crafty discovery fix could treat differently formatted Crafty and Velocity names as separate servers.

**Fixed by:** Reconciling servers through Crafty UUID, Velocity name, normalized name, address, and port. Imported duplicate definitions are archived under `duplicate-backup/<timestamp>/servers`, while the original server and Navigator destination are preserved.

### Admin and player layouts did not match

**Introduced by:** The shared-layout work exposed that the player Navigator and Admin menu used separate placement rules.

**Fixed by:** Rendering both menus from the same `navigator-layout.properties` assignments and reserving Admin control slots.

### Admin server cards stopped opening their editors

**Introduced by:** The first shared-layout fix recalculated the clicked destination from the layout instead of retaining the identity of the displayed card.

**Fixed by:** Binding every rendered Admin card to its exact destination ID and resolving clicks from that stored identity.

### Admin editing threw `NoSuchMethodError`

**Introduced by:** The first Admin click correction was compiled against an inaccurate local Bukkit signature that declared `Player.openInventory` as returning `void`.

**Fixed by:** Rebuilding the listener against Paper's correct `(Inventory)InventoryView` method descriptor.

## Updating

- Core and Hub should both be updated to 1.0.1.
- Restart Velocity after replacing Core.
- Restart the Paper hub server after replacing Hub.
- Interact 1.0.0 and Link 1.0.0 remain compatible, but matching 1.0.1 builds are included.

## Testing status

- Always-On passed live Velocity and Paper testing.
- Manual Stop Server continued to work while Always-On was enabled.
- Navigator progress updated from 0% to 100% without reopening the menu.
- Ping and Online status updated in place.
- Countdown and player transfer completed normally.
- Crafty discovery detected newly created servers without manual Velocity edits.
- Duplicate discovery records were repaired without removing the original servers.
- Admin and player layouts matched.
- Admin left-click editing worked after the final Paper API correction.

HubPilot 1.0.1 completed pre-release testing and is ready for stable use.
