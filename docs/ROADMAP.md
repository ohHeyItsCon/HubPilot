# HubPilot Roadmap

These are planned directions, not release commitments. Items are grouped by the plugins involved.

## Core + Hub

### Guided hub selection

During `/hp setup`, discover the available servers/worlds and ask the owner which server is the hub. Give that server its own hub rules.

This is the planned setup improvement following the 1.0.1 hub-discovery bug. The 1.0.2 prerelease handles the immediate problem through discovery exclusion and automatic shutdown protection; guided selection is still to come.

### Multi-hub and Navigator profiles

Let hubs use different Navigator layouts and server groups. A Minigames Hub could list BedWars, SkyWars, and Parkour, while a Modded Hub lists modpacks. Several physical hubs could share the same profile.

A dedicated **Hub Manager** would keep hub administration separate from the normal backend selector. It would let owners:

- Add or remove hub instances and assign profiles.
- Group identical hubs and choose the main hub or group.
- View health, player counts, and profile assignments.
- Set targets for `/hub`, `/lobby`, and fallback routing.
- Configure hub lifecycle rules, load balancing, and failover.

Core could route players to a healthy or less populated hub in a group, with another hub available if one goes offline.

### Separate multi-hub setup

When multi-hub support arrives, add a **Multi-Hub Setup** path alongside normal single-hub setup.

It would discover hub candidates, let the owner select several hubs, create groups, assign profiles, and choose the default hub. Routing, lifecycle, load balancing, and failover settings would be reviewed before applying the setup.

### Setup presets

Offer starting settings for small, medium, and large networks. Ask about providers, Paper or modded backends, expected startup times, automatic shutdown, and Navigator groups. Every setting would remain editable afterward.

### Navigator folders

Let an entry open another Navigator page. Small networks could organize servers into folders on one hub; multi-hub networks could use those folders within their profiles.

### Server operating modes

**Always-On server shipped in 1.0.1.** It keeps provider startup and manual stop available while disabling automatic idle, failed-request, and queue-empty shutdown.

Possible next modes:

- **On Demand:** a preset for startup, queues, and automatic shutdown.
- **Custom:** owner-defined lifecycle settings.

### Queue controls

The **1.0.2 prerelease** adds editable join and queue messages, visibility controls, formatted names, queue positions, cancellation feedback, previews, placeholders, and global/per-server settings.

Later additions could include wait estimates, queue limits, staff/VIP priority, and better handling of simultaneous requests.

## Core

### Scheduled availability

Set availability windows for events, seasonal servers, weekends, or maintenance. Outside those hours, a server could be hidden, shown as unavailable, or blocked from starting.

### More providers

Add panel integrations where there's demand. AMP, Multicraft, and Pelican are possible candidates. Generic HTTP would continue to cover custom APIs.

### Network history

Keep records of starts, uptime, peak players, average startup time, failed starts, automatic shutdowns, and time spent powered off. Owners could use those records to see which servers get used and how much runtime is saved.

## Hub

- Create, copy, assign, and edit Navigator profiles in-game once multi-hub support exists.
- Build folders and server groups from the admin GUI, including slots, icons, titles, parent menus, and destinations.
- Show what a setup preset will change before the owner accepts it.

## Interact

Let NPCs, signs, portals, entities, and mannequins open a Navigator profile or folder. Add rules for who can use a binding, when it's active, and whether it targets a server, hub, group, or menu.

## Link

Report each hub's player count, response state, and profile to Core for routing, hub groups, load balancing, and failover. This telemetry would stay inside the HubPilot network.

Features will be chosen based on demand, usefulness, implementation risk, and the testing they need.
