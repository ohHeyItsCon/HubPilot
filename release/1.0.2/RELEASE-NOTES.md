# HubPilot 1.0.2: Queue Update

1.0.2 adds an in-game editor for join and queue messages, queue position updates, and more tools for building interactions in the hub. It also includes the repair for hubs that were still shutting down automatically.

This is still a prerelease. The builds passed the simulation tests described below, but live testing on Velocity and Paper is still pending.

## Join and queue messages

Open **Automation Settings → Join & Queue Messages** to change what players see while requesting, waiting for, and joining a server. Set defaults for the whole network or give a server its own messages. Edits take effect without restarting the network.

- **Left-click** to show or hide a message.
- **Right-click** to edit its text, colors, and formatting in chat.
- **Middle-click** to preview it.
- **Shift-right-click** to restore the built-in default or use the global message again.

The editor includes messages for requests, startup, queues, countdowns, joining, retries, cancellation, and errors. It also covers maintenance, disabled automatic startup, wrong client versions, missing permissions, unavailable servers, and players who are already connected or queued. Request cancellation and “no pending request” each have their own message.

These templates change the text for HubPilot's existing events. They don't run commands or add scripted events.

### Queue positions

Players see their position when they join a startup queue. After that, they get an update only when their position changes. Leaving the queue, disconnecting, or using `/hp cancel` updates the positions of everyone still waiting.

### Names and placeholders

`{server}` uses the same bold white display name as the Navigator. Use `{server_plain}` for the display name without that formatting.

Depending on the message, you can also use `{id}`, `{position}`, `{queue_size}`, `{seconds}`, `{attempt}`, `{max}`, `{delay}`, `{required}`, `{current}`, and `{error}`. A placeholder needs data from that event to work.

Both `{placeholder}` and the older `<placeholder>` format work.

## Hub shutdown repair

The September 3 Core build fixes the configured hub being treated like a regular backend and shut down automatically. If `hub-server` already points to the right server, replacing Core and restarting Velocity is all you need to do.

Core matches that setting against managed server IDs and Velocity names, including the equivalent names already recognized by 1.0.2. It then disables idle shutdown, shutdown after a failed request, and shutdown when the queue becomes empty for that hub. The protection applies after the global defaults and saved menu settings, on startup and after a successful configuration reload. It also covers requests that still hold settings from before a reload.

Your hub's saved settings stay in place. Its provider, startup preference, and access settings are kept, and other servers keep their own settings. If you choose a different hub later, the old one uses its saved backend settings again.

**Manual Stop Server still works.** The Hub editor may also still show saved shutdown preferences, but Core overrides those while the server is the configured hub.

The hub is excluded from discovery lists, suggestions, bulk discovery, and import, including common alternate names reported by the controller or Velocity.

The original September 1 Core download only excluded the hub from discovery. It did not include the later shutdown protection. Since both builds are named 1.0.2, check the download against the included SHA-256 list.

## Interact editing tools

### Showing and hiding the tools

Use `/hp adminitem` to put away or bring back Hub's admin item. This works with Hub alone; Interact isn't required.

For the Interact brush:

- `/hpi tool` toggles it.
- `/hpi tool on` shows it.
- `/hpi tool off` hides it.
- `/hpi items` is another way to toggle the brush.

The older combined/admin-target syntax no longer controls Hub's item. Use `/hp adminitem` for that.

Hub remembers when you've hidden the admin item, including after reconnecting, respawning, or updating. Each plugin handles its own tagged item and checks your editing permissions. Other inventory items are left alone. If your inventory is full, free a slot before bringing a missing tool back.

### Editing in the world

Sneak and right-click with the brush to open the Interact editor, then choose a destination and mode:

- **Bind:** right-click a sign or entity to link it.
- **Portal:** left-click and right-click blocks to mark two corners, then choose **Save new portal** in the editor.
- **Inspect:** select an existing interaction to edit.
- **Unbind:** remove a binding while keeping its blocks or entity.
- **Label Up / Label Down:** click a target to move its floating label by 0.25 blocks.

You can also start a named portal with `/hpi portal create <name> <destination>`. Each player has their own selection. Both corners must be in the same world, and portal names must be unique. The existing `/hpi portal pos1`, `pos2`, `save`, and `delete` commands still work.

### Portal styles

Choose **Portal style** in the editor or use:

`/hpi portal type <name> <nether|end|water|invisible>`

Nether, End, and water styles draw particle outlines around the region. Invisible portals have no outline. These are visual styles; they don't place portal or water blocks.

Existing regions use the Nether particle style by default. Your particle on/off and timing settings still apply.

### Floating destination names

Bound signs, entities, and native NPCs get floating destination names, including bindings you've already created. Labels follow moving entities and refresh when destinations change. Portal labels sit in the horizontal center of the region, 1.5 blocks above its bottom. Use the brush to adjust their height.

Removing a binding removes its label. Labels are also cleaned up when the plugin shuts down and don't force chunks to load. Native NPC support depends on the server platform.

To turn labels off, set `destination-labels: false` in Interact's config. Run `/hpi reload` to reload the config and bindings.

## Other fixes

- Hiding countdown text no longer mutes its sound or leaves a blank chat or action-bar line.
- Countdown messages on the Paper hub keep your chosen colors instead of being forced to yellow.
- Global and per-server message edits now get through the Hub-to-Core settings filter. Credentials, unknown fields, and unrelated settings are still rejected.

## Updating

Use the latest files attached to this prerelease. Some JARs were replaced while keeping the 1.0.2 version number, so use the included SHA-256 list to check which builds you have.

- **Coming from 1.0.1:** update Core on Velocity and Hub on the Paper hub together. Update Interact too if you want the new brush, portal, and label features.
- **Already using the September 3 Core repair:** keep that Core build. Replace Hub and Interact on the Paper hub for the newer Interact features. Any ongoing Core idle-shutdown test can continue.
- **Only applying the Core shutdown repair:** replace Core and restart Velocity. An existing Hub 1.0.2 installation is sufficient for that repair.

Restart Velocity after replacing Core, and restart the Paper hub after replacing Hub or Interact.

Link has no functional changes; Link 1.0.1 remains compatible, and a matching 1.0.2 build is included. Interact was also unchanged in the original Queue Update, but the latest prerelease includes the new features described above.

### Existing settings

Your configuration loads automatically. Existing flat entries in `messages/en_US.yml` from 1.0.1 still work; new installations get the event-based defaults. In-game message edits are saved with HubPilot's settings and sent to Core through `hubpilot:settings`.

You can keep your server, provider, discovery, destination, layout, and telemetry files.

The 1.0.1 features remain available: per-server Always-On, manual start and stop, Crafty discovery, dynamic Velocity registration, duplicate discovery repair, and live Navigator refresh. The shared Admin/player layout, Admin left-click editing, and Paper `openInventory` fix are carried forward too.

## Testing so far

The existing test reports record passes for Core message rendering, legacy message files, global and per-server visibility, and Hub message saving, reset, and inheritance. Message-key filtering, configured-hub shutdown protection, and packaged classes and metadata also passed their controlled checks.

The latest Hub and Interact JARs passed Paper 1.21.10 simulation tests covering:

- Portal saving, legacy defaults, separate corner selections, and brush-based creation.
- Permissions, tool toggles, hidden-admin restoration, and full inventories.
- Label placement, moving entities, duplicate prevention, and cleanup.

The simulation needed test-only setters for display styles. It does not establish how those displays look or respond in an actual client.

**Live testing is still pending:** the message editor, queue positions with multiple players, hub shutdown protection, Interact's in-game appearance and controls, and a full Paper startup still need checks on a running Velocity/Paper network before this becomes a stable release.
