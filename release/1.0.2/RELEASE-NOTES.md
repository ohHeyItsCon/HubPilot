# HubPilot 1.0.2: Queue Update

The queue messages can now be edited in-game. Players can see their place in line while a server starts, and Interact has a few new tools for setting up portals and labels around the hub.

This download also includes the September 3 hub shutdown repair. If your hub kept turning itself off on an earlier 1.0.2 build, see the update instructions below.

1.0.2 is still a prerelease. Simulation tests have passed, but the live Velocity/Paper checks aren't finished yet.

## Changing the messages

Go to **Automation Settings**, then **Join & Queue Messages**. You can change the network defaults there or set different messages for an individual server. There's no restart needed after an edit.

- Left-click a message to turn it on or off.
- Right-click to write new text in chat, including colors and formatting.
- Middle-click for a preview.
- Shift-right-click to go back to the built-in default or inherit the global message.

This covers the messages players see when they request a server, wait for it to start, join the queue, count down, and connect. Retry, failure, and cancellation messages are editable too, along with notices about maintenance, permissions, client versions, unavailable servers, and disabled automatic startup. There are separate messages for being already connected or queued, canceling a request, and having nothing to cancel.

The editor changes those existing messages. It doesn't add commands or scripted events.

A player gets their queue position when they enter the queue. They'll hear about it again if that position changes. When someone leaves, disconnects, or runs `/hp cancel`, everyone behind them moves up.

For server names, `{server}` now uses the Navigator's bold white display name. Choose `{server_plain}` if you want the name without that formatting.

You can also use `{id}`, `{position}`, `{queue_size}`, `{seconds}`, `{attempt}`, `{max}`, `{delay}`, `{required}`, `{current}`, and `{error}` where the message has that information available. Existing templates using `<placeholder>` will still work.

## The hub shutting down

After the 1.0.1 discovery changes, the hub could end up being managed like any other server. Excluding it from discovery wasn't enough to fix that for hubs already in the configuration.

The September 3 Core build disables all three automatic shutdown rules for the server named in `hub-server`: idle shutdown, stopping after a failed request, and stopping when the queue empties. If that setting already names the right hub, **you don't need to change any settings. Replace Core and restart Velocity.**

Core looks for the hub among its managed server IDs and Velocity names. It uses the same alternate-name matching already in 1.0.2.

The saved shutdown settings can still appear in the Hub editor. Core overrides them for the hub when it starts or successfully reloads its configuration. A request left over from before the reload can't bypass that protection.

Those saved settings aren't deleted. If you pick a different hub later, the former hub goes back to using them. The repair also keeps its provider, startup preference, and access settings.

You can still stop the hub yourself with **Stop Server**. Other servers follow their own rules.

Discovery also leaves the hub out of its lists, suggestions, bulk discovery, and imports, including common alternate names from the controller or Velocity.

## Putting the editing items away

`/hp adminitem` now puts away or brings back Hub's admin item. You don't need Interact installed for that command.

Use `/hpi tool` or `/hpi items` for Interact's brush. If you'd rather specify which way it should go, use `/hpi tool on` or `/hpi tool off`.

The older combined/admin-target commands no longer affect Hub's item. That's handled by `/hp adminitem`.

Once you've hidden the admin item, it stays hidden through reconnects, respawns, and this update. The commands still require editing permissions and only affect the plugin's own tagged item. If your inventory is full, you'll need to free a slot to bring a missing tool back.

## Building interactions with the brush

Sneak and right-click with the brush to open the Interact editor. Pick a destination, then choose what you want to do:

- **Bind:** right-click a sign or entity to link it.
- **Portal:** mark two corners with left- and right-clicks on blocks, then open the editor and choose **Save new portal**.
- **Inspect:** select an interaction to edit.
- **Unbind:** remove the link. The blocks or entity stay there.
- **Label Up / Label Down:** click a target to move its label by 0.25 blocks.

To name a portal before selecting it, use `/hpi portal create <name> <destination>`. Selections belong to each player, so another editor won't replace yours. Both corners need to be in the same world, and you can't reuse a portal name.

The older `/hpi portal pos1`, `pos2`, `save`, and `delete` commands are still available.

### Portal appearance

Pick **Portal style** in the editor, or run:

`/hpi portal type <name> <nether|end|water|invisible>`

The Nether, End, and water options draw particle outlines. Invisible hides the outline altogether. None of these place actual portal or water blocks.

Portals you've already made default to the Nether style. Existing particle on/off and timing settings still work.

### Destination labels

Bound signs, entities, and native NPCs now show the destination name above them. Existing bindings get labels automatically. NPC availability still depends on the server platform.

An entity's label follows it as it moves. A portal's label starts in the horizontal center, 1.5 blocks above the bottom of the region. You can adjust the height with the brush.

Changing the destination updates the label; removing the binding removes it. Labels are also cleared when the plugin shuts down, and they don't force chunks to load.

Don't want them? Set `destination-labels: false` in Interact's config, then run `/hpi reload`. That command reloads both the config and bindings.

## A few smaller fixes

Hiding the countdown text used to silence its sound as well. The sound now plays independently, and a hidden message won't leave an empty chat or action-bar line.

Countdown colors also work on the Paper hub now. It was forcing the text to yellow.

The settings filter was dropping some message edits between Hub and Core. It now accepts the defined global and per-server message keys. Credentials, unknown fields, and unrelated settings are still rejected.

## Which files to replace

**Check the included SHA-256 list.** The September 1 Core download only had the discovery exclusion, and later JARs kept the same 1.0.2 filenames.

Coming from 1.0.1? Replace Core on Velocity and Hub on the Paper hub together. Replace Interact too for the new brush, portal, and label features.

Already have the September 3 Core repair? Keep it and replace Hub and Interact for the newer editing tools. You can continue an idle-shutdown test on that Core build.

If you're only installing the Core shutdown repair, an existing Hub 1.0.2 is fine.

Restart Velocity after replacing Core. Restart the Paper hub after replacing Hub or Interact.

Link hasn't changed apart from its version number. Link 1.0.1 still works, and a matching 1.0.2 is included. Interact was also a version-only update in the original Queue Update download; the newer download includes the features above.

There's no need to replace your configuration files. That includes the old flat entries in `messages/en_US.yml`; 1.0.2 can still read them. Fresh installs use the new event-based defaults.

Messages edited in-game are saved with the rest of HubPilot's settings. Hub sends those edits to Core over `hubpilot:settings`.

The Always-On option, manual power controls, Crafty discovery and registration, duplicate repair, live Navigator refresh, shared menu layouts, Admin editing, and Paper `openInventory` fix from 1.0.1 are still included.

## What has been tested

The test reports show passes for rendering and saving messages, reading old message files, hiding messages, resetting them, and inheriting defaults. The message-key filter and hub shutdown protection passed their controlled checks. The packaged classes and metadata were checked as well.

For the newer Hub and Interact JARs, the Paper 1.21.10 simulation covered saving portals, loading legacy defaults, selecting corners with the brush, and checking permissions. It also covered putting tools away and restoring them, keeping hidden items hidden, and handling a full inventory. Label checks covered placement, movement, duplicates, and cleanup.

The display-style setters used in that simulation were added for testing. Actual appearance and client interaction still need an in-game check.

The remaining live tests are the message editor, a queue with multiple players, the hub shutdown fix, and Interact's controls and appearance. A full Paper startup also needs checking on the running Velocity/Paper network. Until those are done, this stays a prerelease.
