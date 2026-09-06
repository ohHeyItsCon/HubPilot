# HubPilot 1.0.2: Queue Update

1.0.2 started with the queue and message editor. This prerelease also includes Interact's portal and label tools, plus the September 3 repair for hubs that were still shutting down.

Live Velocity/Paper testing is still pending. The checks completed so far are listed at the end.

## Changing the messages

Join and queue messages have their own editor under **Automation Settings → Join & Queue Messages**. Set them once for the network, or override them for a particular server. Changes apply without a restart.

- Left-click a message to turn it on or off.
- Right-click to write new text in chat, including colors and formatting.
- Middle-click for a preview.
- Shift-right-click to go back to the built-in default or inherit the global message.

The editable messages cover the full join process, from sending a request through startup, queuing, the countdown, and connecting. You can also change retries, errors, maintenance notices, and permission or client-version warnings. That includes unavailable servers, disabled automatic startup, and notices that a player is already connected or queued.

`/hp cancel` has two messages to edit: one for canceling a request, and one for having nothing to cancel. Templates are limited to HubPilot's existing events and can't run commands.

Players see their position on entering a startup queue. Further updates are sent only when it changes, including when someone ahead of them leaves, disconnects, or uses `/hp cancel`.

For server names, `{server}` now uses the Navigator's bold white display name. Choose `{server_plain}` if you want the name without that formatting.

Other available placeholders: `{id}`, `{position}`, `{queue_size}`, `{seconds}`, `{attempt}`, `{max}`, `{delay}`, `{required}`, `{current}`, and `{error}`. Each needs the corresponding data from the event. The older `<placeholder>` format is also accepted.

## Hub shutdown fix

The 1.0.1 discovery changes allowed the hub to be imported and inherit automatic shutdown settings. Removing it from discovery didn't solve the problem for hubs that had already been imported.

With the September 3 Core build, the server named in `hub-server` is exempt from idle shutdown, stopping after a failed request, and stopping when its queue empties. **Replace Core and restart Velocity.** No configuration change is needed if `hub-server` already identifies the hub correctly.

Core matches managed IDs and Velocity names using 1.0.2's existing alternate-name rules. It applies the exemption after global defaults and menu settings, both at startup and on a successful reload. Requests holding older settings are covered as well.

The Hub editor may still show saved shutdown settings. They won't apply while that server is the configured hub. Those settings are kept for use if you later choose a different hub; its provider, startup preference, and access settings are kept too.

You can still stop the hub yourself with **Stop Server**. Other servers follow their own rules.

Discovery also leaves the hub out of its lists, suggestions, bulk discovery, and imports, including common alternate names from the controller or Velocity.

## Putting the editing items away

`/hp adminitem` now puts away or brings back Hub's admin item. You don't need Interact installed for that command.

`/hpi tool` and its alias `/hpi items` toggle Interact's brush. Use `/hpi tool on` to show it or `/hpi tool off` to hide it. The older combined/admin-target syntax no longer controls Hub's admin item; use `/hp adminitem` for that.

Hub remembers a hidden admin item after reconnects, respawns, and updating. Editing permissions still apply. Each command handles its plugin's tagged item and leaves ordinary items alone, so bringing a tool back requires a free inventory slot.

## Building interactions with the brush

Sneak and right-click with the brush to open the Interact editor. Pick a destination, then choose what you want to do:

- **Bind:** right-click a sign or entity to link it.
- **Portal:** mark two corners with left- and right-clicks on blocks, then open the editor and choose **Save new portal**.
- **Inspect:** select an interaction to edit.
- **Unbind:** remove the link. The blocks or entity stay there.
- **Label Up / Label Down:** click a target to move its label by 0.25 blocks.

`/hpi portal create <name> <destination>` starts a named selection. Each player has a separate selection. Portal names must be unique, with both corners in the same world.

The older `/hpi portal pos1`, `pos2`, `save`, and `delete` commands are still available.

### Portal appearance

Pick **Portal style** in the editor, or run:

`/hpi portal type <name> <nether|end|water|invisible>`

Nether, End, and water styles outline the region with particles; invisible has no outline. These options don't place real portal or water blocks. Older portals default to Nether particles and keep the existing particle on/off and timing settings.

### Destination labels

Bound signs, entities, and native NPCs now show the destination name above them. Existing bindings get labels automatically. NPC availability still depends on the server platform.

An entity's label follows it as it moves. A portal's label starts in the horizontal center, 1.5 blocks above the bottom of the region. You can adjust the height with the brush.

Labels update when destinations change and disappear when bindings are removed or the plugin shuts down. They don't force chunks to load.

To disable them, set `destination-labels: false` in Interact's config. `/hpi reload` reloads that config and the bindings.

## Other fixes

- Hiding countdown text no longer silences the sound or leaves a blank chat or action-bar line.
- The Paper hub uses the countdown template's colors instead of forcing yellow.
- The Hub-to-Core filter accepts the defined global and per-server message keys, fixing edits being dropped during sync. It still rejects credentials, unknown fields, and unrelated settings.

## Which files to replace

**Check the included SHA-256 list.** The September 1 Core download only had the discovery exclusion, and later JARs kept the same 1.0.2 filenames.

- **From 1.0.1:** replace Core on Velocity and Hub on the Paper hub together. Update Interact for the new editing tools.
- **From the September 3 Core repair:** keep Core and replace Hub and Interact. An ongoing idle-shutdown test can continue on that Core build.
- **For the Core shutdown repair alone:** replace Core. Hub 1.0.2 doesn't need replacing for this fix.

Restart Velocity after replacing Core. Restart the Paper hub after replacing Hub or Interact.

Link hasn't changed apart from its version number. Link 1.0.1 still works, and a matching 1.0.2 is included. Interact was also a version-only update in the original Queue Update download; the newer download includes the features above.

You can keep your existing configuration files. Flat `messages/en_US.yml` entries from 1.0.1 still load, while fresh installs get the new event-based defaults. In-game edits are saved with HubPilot's settings and sent to Core through `hubpilot:settings`.

## What has been tested

Core and Hub passed the recorded checks for message rendering and storage, legacy files, visibility, reset, inheritance, and filtering. Controlled checks also passed for the hub shutdown exemption and packaged classes and metadata.

The latest Hub and Interact JARs were tested in a Paper 1.21.10 simulation. Those checks covered portal saving and legacy defaults, brush selections, permissions, tool toggles, hidden items, full inventories, label placement and movement, duplicate prevention, and cleanup.

The simulation required test-only display-style setters, so actual client appearance and interaction remain unverified.

Before a stable release, the message editor, queue positions with multiple players, hub shutdown repair, Interact controls and appearance, and a full Paper startup still need testing on a running Velocity/Paper network.
