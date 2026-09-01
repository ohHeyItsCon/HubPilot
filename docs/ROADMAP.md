# HubPilot Roadmap

These are ideas being considered for future versions. They are not promises or assigned to a release until they move into an actual release plan.

The roadmap is grouped by the component that would handle most of the work.

## Core + Hub

### Multi-hub and Navigator profiles

Let different hubs use different Navigator layouts and server groups.

```text
Main Hub
├── Survival Hub
├── Minigames Hub
└── Modded Hub

Minigames Hub
├── BedWars
├── SkyWars
└── Parkour
```

Several physical hubs could share one profile, which would also give larger networks a base for load balancing and redundant hub instances.

Multi-hub management should use a dedicated **Hub Manager** instead of mixing hub instances into the normal server selector. The normal server selector should stay focused on playable backend destinations, while the Hub Manager handles things that only apply to hubs.

Possible Hub Manager controls include:

- add or remove hub instances
- assign a Navigator profile to each hub
- place several physical hubs into one logical hub group
- choose the main/default hub or hub group
- view hub health, player count, and current profile
- configure load-balancing and failover behavior
- choose which hubs can be used by `/hub`, `/lobby`, and fallback routing

That keeps a large network with several hub instances from turning the main server management GUI into one mixed list of hubs and game servers.

### Nested Navigator folders

Let Navigator entries open another Navigator page instead of always pointing straight to a server. Small networks could use folders on one hub, while larger networks could use the same profile system across real sub-hubs.

### Hub groups, load balancing, and failover

Treat several hub servers as one group. Core could pick the healthiest or least-populated hub for `/hub`, `/lobby`, and fallback routing, then move players to another hub if one goes offline.

### Per-server operating modes

**Always-On server shipped in 1.0.1.** It keeps provider startup and manual stop available while disabling HubPilot's automatic idle, failed-request, and queue-empty shutdown behavior for that server.

The broader operating-mode idea is still on the roadmap. Possible additions include:

- **On Demand:** use startup, queue, idle, and failed-request automation as a preset
- **Custom:** let the owner build a lifecycle policy without using a preset

A mixed network could eventually look like:

```text
Main Hub       -> Always On
BedWars        -> Always On
Events         -> On Demand
Modded SMP     -> On Demand
Seasonal       -> Custom
```

### Guided setup and network-size presets

Expand `/hp setup` so a new install can start with useful defaults based on its network.

As part of setup, HubPilot should perform an initial server/world discovery and ask the owner which discovered server is the hub. The selected hub would then be treated as a hub role instead of a normal managed backend and receive its own lifecycle rules. This is the current planned fix for the 1.0.1 bug where the hub can appear in `/hp discover` and inherit global backend auto-stop settings.

This hub selection should also provide the starting point for future multi-hub support, where more than one discovered hub can be assigned to the Hub Manager and given hub-specific profiles, routing, lifecycle, and failover rules.

When multi-hub support is released, setup should also offer a separate **Multi-Hub Setup** path instead of forcing larger networks through the normal single-hub flow. That setup would be focused on identifying and organizing several hubs at once.

Possible Multi-Hub Setup steps include:

- discover available hub candidates
- select every server that should be treated as a hub
- choose the main/default hub or hub group
- create hub groups for identical or load-balanced hub instances
- assign a Navigator profile to each hub or group
- choose `/hub`, `/lobby`, and fallback routing targets
- configure hub-specific lifecycle rules
- configure load balancing and failover defaults
- review the resulting Hub Manager layout before applying it

The normal setup path would stay simple for single-hub networks, while Multi-Hub Setup would handle the extra decisions that only matter when several hubs are present.

Possible questions:

- small, medium, or large network?
- one hub or several?
- which discovered server is the hub?
- mostly Always On, On Demand, or mixed?
- which power provider?
- Paper, modded, or mixed backends?
- normal startup time?
- should idle shutdown be enabled?
- one Navigator or several groups?

The result would only be a starting preset. Every setting would still be editable afterward.

### Better queue controls

**The first queue improvements shipped in the 1.0.2 Queue Update pre-release.** They include configurable join-flow messages, message visibility, formatted server names, queue position, cancellation feedback, previews, placeholders, and global/per-server inheritance.

Later queue additions may include estimated wait time, queue limits, staff/VIP priority, and more advanced handling when several players request the same offline server.

## Core

### Scheduled server availability

Allow event, seasonal, weekend-only, or maintenance servers to become available on a schedule. A server could be hidden, shown as unavailable, or blocked from starting outside its configured window.

### More server-management providers

Add direct integrations when there is real demand. AMP, Multicraft, Pelican, and other common panels are possible candidates. Generic HTTP would remain available for custom APIs.

### Network statistics and history

Keep longer-term information such as:

- server starts
- uptime
- peak players
- average startup time
- failed starts
- idle/failed-request shutdowns
- time spent powered off

This could make HubPilot's resource savings measurable instead of only visible in the moment.

## Hub

### Navigator profile editor

Create, copy, assign, and edit Navigator profiles in-game once multi-hub support exists.

### Server-group and folder editing

Build nested folders and server groups from the admin GUI, including slots, icons, titles, parent menus, and destinations.

### Network preset review

Show what a Small, Medium, or Large setup preset is about to change before the owner accepts it.

## Interact

### Navigator and profile targets

Let an NPC, sign, portal, entity, or mannequin open a Navigator profile/folder instead of only requesting one server.

### More flexible interaction rules

Add more control over who can use a binding, when it is active, and whether it points to a server, hub, group, or Navigator page.

## Link

### Multi-hub telemetry sync

Let several Hub servers report their local state to Core for hub groups, load balancing, and failover.

### Hub health reporting

Send lightweight hub health such as player count, response state, and assigned Navigator profile so Core can make better routing choices.

HubPilot telemetry would stay inside the HubPilot network and would not be sent to an outside analytics/tracking service.

## Versioning and component compatibility

HubPilot keeps one public release number across Core, Hub, Interact, and Link.

Core and Hub should normally run the exact same version. Link and Interact can stay on an older compatible build when the release notes say so, but should not be more than two published releases behind.

## How roadmap items are chosen

Features move forward based on usefulness, implementation risk, testing needs, and feedback from real HubPilot networks.
