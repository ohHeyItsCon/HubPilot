# HubPilot 1.0.2 — Queue Update

HubPilot 1.0.2 makes the server request and startup queue easier to understand and easier to customize.

This is a pre-release. The packaged builds have passed controlled validation, but the new message editor and multi-player queue-position flow still need a short live test on Velocity and Paper before 1.0.2 is promoted to stable.

## Join and queue message controls

The messages shown while HubPilot starts and joins a server can now be managed in-game.

- Open **Automation Settings**, then select **Join & Queue Messages**.
- Left-click a message to show or hide it.
- Right-click a message to edit its text, colors, and formatting in chat.
- Middle-click a message to preview it.
- Shift-right-click a message to restore the built-in default or inherit the global message.
- Configure global defaults or give an individual server its own overrides.
- Changes use the existing trusted Hub-to-Core settings channel and do not require a network restart.

The editor covers these events:

- already connected
- request sent
- server unavailable
- automatic startup disabled
- server starting
- server already starting
- queue joined
- already queued
- queue position changed
- server ready
- countdown
- joining
- connection failed
- retrying
- request canceled
- no pending request
- start failed
- maintenance
- wrong client version
- missing permission

Templates remain tied to known HubPilot events. This update does not add arbitrary commands or event scripting.

## Queue feedback

- Players are told their position when they enter a startup queue.
- Position updates are sent only when a player's position actually changes.
- Leaving, disconnecting, or using `/hp cancel` updates the remaining players' positions.
- `/hp cancel` now has separate configurable messages for a canceled request and for having no pending request.

## Server-name formatting

`{server}` now uses the same bold white server-name style as the Navigator item instead of showing an unformatted internal name.

`{server_plain}` is also available when a template needs the display name without Navigator formatting.

Other placeholders are available where the event supplies them:

- `{id}`
- `{position}`
- `{queue_size}`
- `{seconds}`
- `{attempt}`
- `{max}`
- `{delay}`
- `{required}`
- `{current}`
- `{error}`

Both `{placeholder}` and the older `<placeholder>` style are accepted.

## Configuration compatibility

- Existing flat `messages/en_US.yml` entries from 1.0.1 remain supported.
- Fresh installations receive the new event-based message defaults.
- In-game edits are stored with the existing HubPilot settings and synchronized to Core through `hubpilot:settings`.
- Core continues to reject unrelated or sensitive keys from the settings payload.
- Existing server, provider, discovery, destination, layout, and telemetry files do not need to be replaced.

## Bug fixes

### Hiding the countdown message also silenced its sound

Found while adding message visibility controls. Countdown text and sound are now handled separately, so hiding the message does not silence the configured sound or produce a blank chat or action-bar line.

### Countdown colors were lost on the Paper hub

Found while validating editable countdown templates. The Paper feedback bridge now applies the template's color formatting and no longer forces yellow.

### Message settings could be dropped by the existing sync filter

Found while validating Hub-to-Core message synchronization. The trusted filter now accepts the defined global and per-server message keys while continuing to reject credentials, unknown fields, and unrelated settings.

### The configured hub could appear in server discovery

Found after the 1.0.1 discovery changes. Discovery now excludes the configured hub from lists, suggestions, bulk discovery, and final import, including common equivalent names reported by a controller or Velocity.

### The configured hub could inherit automatic idle shutdown

Found while reviewing the discovery regression. Core now exempts the configured `hub-server`, including common equivalent names, from automatic idle shutdown. This protects the single configured main hub without changing manual Stop Server or normal backend lifecycle behavior.

## Preserved 1.0.1 behavior

- per-server Always-On behavior
- manual provider start and stop controls
- Crafty discovery and dynamic Velocity registration
- duplicate discovery repair
- live Navigator telemetry refresh
- shared Admin and player Navigator layouts
- Admin left-click server editing
- the corrected Paper `openInventory` runtime signature

## Interact portals, destination labels, and editing tools

The latest 1.0.2 prerelease includes the Interact expansion. Replace **Hub** and **Interact** on the Paper hub and restart it. Core's September 3 hub-protection build is unchanged, so an existing idle-shutdown test can continue on that Core. Link is unchanged. Check the included SHA-256 list because the version remains 1.0.2.

### Put editing items away

- `/hp adminitem` toggles Hub's admin item. This is implemented in Hub and works without Interact installed; Core continues to forward the existing command unchanged.
- `/hpi tool` toggles only Interact's brush.
- `/hpi tool on` and `/hpi tool off` explicitly show or hide the brush.
- `/hpi items` is a brush-only alias. The earlier combined/admin-target syntax no longer controls Hub items; use `/hp adminitem` instead.

The hidden-admin preference remains saved on the player and is respected by Hub's automatic restoration, including reconnects and respawns. Existing hidden preferences survive this update. Each component manages only its own tagged item. Editing permissions are checked, ordinary items are left intact, and a full inventory is never overwritten. Free a slot to show a missing item.

### In-world editing

Sneak + right-click the brush to open the Interact editor. Select a destination and a mode:

- **Bind:** right-click a sign or entity to link it.
- **Portal:** left-click and right-click blocks to select two corners; open the editor and choose **Save new portal**.
- **Inspect:** select an existing interaction for editing.
- **Unbind:** remove the selected interaction without deleting its blocks or entity.
- **Label Up / Label Down:** click a target to adjust its floating label by 0.25 blocks.

Use `/hpi portal create <name> <destination>` to start a named portal selection. Each editor has their own selection. Cross-world selections and duplicate names are rejected. Existing `/hpi portal pos1`, `pos2`, `save`, and `delete` commands remain available.

### Portal styles

Use the editor's **Portal style** control or `/hpi portal type <name> <nether|end|water|invisible>`.

Styles use portal-themed particle outlines around the selected region. Invisible regions have no particle outline. They do not replace blocks with real Nether portal, End portal, or water blocks. Existing portal regions keep the Nether particle style by default. Existing particle enable/period settings remain supported.

### Floating destination names

Text displays show the current destination label above bound signs, entities, and native NPCs. Existing bindings receive labels automatically. Entity labels follow their targets; portal labels use the horizontal center of the selected region, 1.5 blocks above its bottom. Label height can be adjusted with the brush. Labels refresh when destinations change and disappear when their bindings are removed. Labels are temporary, cleaned up on plugin shutdown, and do not force-load chunks.

Set `destination-labels: false` in Interact's config to disable them; `/hpi reload` reloads configuration and bindings. Native NPC availability still depends on the server platform.

### Validation

The exact packaged Hub and Interact passed controlled Paper 1.21.10 simulation tests for portal persistence, legacy defaults, permission checks, inventory toggles, hidden-admin restoration, full inventories, brush corner selection and saving, centered labels, entity movement, duplicate prevention, and cleanup. The test framework required test-only display-style setters. Actual in-game appearance, client interaction and a complete live Paper startup remain to be field-tested; this remains a prerelease.

## Updating

### September 3 automatic hub repair (version remains 1.0.2)

Replace Core on Velocity with the repaired Core JAR and restart Velocity. Already running Hub 1.0.2 does not need replacement for this repair. Upgrading from 1.0.1 still requires the existing Hub 1.0.2 for Queue Update compatibility.

On startup and each successful configuration reload, Core matches `hub-server` against managed IDs and Velocity names, using existing 1.0.2 equivalent-name rules. It applies a protected role after all defaults and GUI overrides: idle minutes become zero, stop-after-failure becomes false, and stop-when-queue-empty becomes false. A final automatic-stop guard covers sessions holding pre-reload definitions. Discovery exclusion remains intact.

No manual configuration step is needed when `hub-server` correctly identifies the hub. The repair retains the managed entry and saved configuration instead of deleting or archiving it. Provider identity, startup preference, access settings, and unrelated servers remain intact. Changing the configured hub later restores the former hub's saved backend settings. Manual Stop Server is still intentional and available. The Hub editor may still display saved backend preferences; Core's protected role overrides them while that entry is the configured hub.

The original September 1 published Core only contained the discovery exclusion; it did not contain the later local idle-only guard. Use this build's checksum to verify the repaired download. This package remains a pre-release. Controlled packaged tests passed; live Velocity/Paper field testing is pending.

### Original Queue Update upgrade

Update Core and Hub together:

- `HubPilot-Core-1.0.2.jar` on Velocity
- `HubPilot-Hub-1.0.2.jar` on the Paper hub

HubPilot Link and HubPilot Interact have no functional changes in this update. Their 1.0.1 builds remain compatible. Matching 1.0.2 builds are included so the installed suite can use one version number.

Restart Velocity and the hub after replacing the JARs. Existing configuration is loaded automatically.

## Testing status

- exact packaged Core message rendering and inheritance: passed
- legacy flat-message compatibility: passed
- global and per-server show/hide behavior: passed
- Hub message storage, persistence, reset, and inheritance: passed
- trusted message-key filtering: passed
- configured-hub idle-shutdown protection: passed
- packaged class and metadata inspection: passed
- Link and Interact version-only equivalence: passed
- live Velocity and Paper field test: pending
