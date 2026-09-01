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

## Bugs found and fixed during this update

### Hiding the countdown message also silenced its sound

**Found after:** Message visibility controls were added to the countdown path.

The first implementation stopped the whole countdown announcement when its text was hidden. That also stopped the existing countdown sound even though only the message had been disabled.

**Fixed:** Countdown text and countdown sound are now handled separately. A hidden countdown message produces no blank chat or action-bar line, while the configured sound continues normally.

### Countdown colors were lost on the Paper hub

**Found after:** Countdown messages were changed from fixed text to editable templates.

The Hub feedback bridge treated the received template as plain text and forced a yellow prefix, so custom colors could be ignored or displayed incorrectly.

**Fixed:** The bridge now applies the template's legacy color formatting before displaying the action bar and no longer forces one color.

### Message settings could be dropped by the existing sync filter

**Found after:** The message editor was connected to the existing Hub-to-Core settings channel.

The original filter only accepted the automation keys used by 1.0.1. New message keys would have been removed before Core reloaded them.

**Fixed:** The filter now accepts only the defined global and per-server message-key shapes while continuing to reject provider credentials, unknown fields, and unrelated settings.

### The configured hub could appear in server discovery

**Found in:** HubPilot 1.0.1 discovery testing after the stable release.

The configured hub could be offered as a normal backend by `/hp discover`. If it was imported, it could inherit lifecycle defaults intended for game servers.

**Fixed:** Discovery now excludes the configured hub from lists, suggestions, bulk discovery, and the final import path. It also recognizes common equivalent hub names reported by a controller or existing Velocity registration.

## Preserved 1.0.1 behavior

- per-server Always-On behavior
- manual provider start and stop controls
- Crafty discovery and dynamic Velocity registration
- duplicate discovery repair
- live Navigator telemetry refresh
- shared Admin and player Navigator layouts
- Admin left-click server editing
- the corrected Paper `openInventory` runtime signature

## Updating

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
- packaged class and metadata inspection: passed
- Link and Interact version-only equivalence: passed
- live Velocity and Paper field test: pending
