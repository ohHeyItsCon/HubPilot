# HubPilot Roadmap

These are ideas being considered for future HubPilot versions. They are not promises, and they are not tied to a specific release until they move into an actual release plan.

The roadmap is split by component so it is easier to see where each idea would live. Bigger features that need both sides of the suite are under **Core + Hub**.

## Core + Hub

### Multi-hub and Navigator profiles

Let different hubs use different Navigator layouts and server groups.

A main hub could send players to a Survival Hub, Minigames Hub, or Modded Hub. Each of those hubs could then have its own Navigator for the servers that belong there.

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

Several physical hub servers could also share one Navigator profile. That would let a larger network run multiple copies of the same hub without rebuilding the menu on each one.

Core would own the routing, profile, server-group, and hub-target logic. Hub would show and edit the right Navigator for the hub the player is on.

### Nested Navigator folders

Let a Navigator entry open another Navigator page instead of always pointing straight to a server.

That gives a smaller network folders without forcing it to run extra hub servers.

```text
Server Navigator
├── Survival
├── Minigames >
└── Modded >

Minigames >
├── BedWars
├── SkyWars
└── Parkour
```

The same profile system could power both nested menus and real sub-hubs so the two features do not need separate foundations.

### Hub groups and load balancing

Treat several hubs as one logical group.

For example, `hub-1`, `hub-2`, and `hub-3` could all use the same profile. When somebody uses `/hub` or needs a fallback, Core could pick the healthiest or least-populated hub.

Hub would keep those instances on the same logical profile and admin layout.

### Hub failover

Give a hub group backup behavior.

If `hub-1` goes down, HubPilot could send returning players to `hub-2` instead of leaving `/hub`, `/lobby`, or another return-to-hub path broken.

This would work alongside load balancing so large networks can have both capacity and redundancy.

### Per-server operating modes

Give each managed server an operating mode so HubPilot knows how that server is supposed to behave.

This is different from the provider. The provider answers **how HubPilot talks to the process**. The operating mode answers **what HubPilot should do with that server day to day**.

Possible modes:

#### Always On

The server is expected to stay running.

HubPilot would not idle-shutdown it or stop it after a failed player request. This fits large networks that have enough hardware to keep popular worlds, minigames, or hubs online all the time.

An Always On server could still use Crafty, Pterodactyl, Generic HTTP, or another provider so HubPilot can bring it back after a crash or manual stop.

#### On Demand

The server can stay offline until somebody asks for it.

HubPilot can start it, wait for it to come up, move players over, then shut it down later using the configured idle and failed-request rules.

This is the better fit for small networks or heavier backends that do not need to run all day.

#### Custom

Let the owner choose the lifecycle behavior manually.

That could mix automatic startup, idle shutdown, failed-request shutdown, queue behavior, retries, and timeout settings without forcing the server into a preset.

A mixed network could look like this:

```text
Main Hub       -> Always On
Minigames Hub  -> Always On
BedWars        -> Always On
SkyWars        -> Always On
Events         -> On Demand
Modded SMP     -> On Demand
Seasonal       -> On Demand
```

### Guided setup and network-size presets

Expand `/hp setup` so it asks a few questions before writing the first defaults.

The point is to get a new install close to a sensible setup without making the owner tune every timeout, shutdown rule, queue setting, and Navigator option by hand before anything is usable.

Possible questions:

- Is the network small, medium, or large?
- Is there one hub or several?
- Should most servers be Always On, On Demand, or mixed?
- Which power provider manages the servers?
- Are the backends mostly lightweight Paper servers, heavily modded servers, or mixed?
- About how long do they take to start?
- Should idle shutdown be on by default?
- Does the network need several Navigator groups or one main menu?

HubPilot could turn those answers into a starting preset.

#### Small network preset

One hub, a simple Navigator, mostly On Demand backends, shorter idle timers, smaller queues, and stronger resource saving.

#### Medium network preset

A mix of Always On and On Demand servers, longer startup timeouts, larger queues, server categories, and room for a second hub or more complex Navigator layout.

#### Large network preset

Several hubs, more Always On servers, larger queues, server-specific timeouts, hub groups, failover, and little or no automatic shutdown unless the owner wants it.

These presets would only set starting values. Nothing would be locked afterward.

### Better queue controls

Build the current offline-server request flow into a more complete queue system.

Possible additions:

- queue position
- estimated wait time
- maximum queue size
- canceling a request
- staff or VIP priority
- better handling when many players request the same offline server
- clearer behavior when a server fails to start

Core would own the queue state. Hub would show it to players and staff.

## Core

### Scheduled server availability

Let servers become available or unavailable on a schedule.

This could cover event servers, seasonal worlds, weekend-only servers, maintenance windows, or servers that should only be usable during certain hours.

A scheduled server could be hidden, shown as unavailable, or blocked from starting outside its schedule.

### More server-management providers

Add direct support for more panels when people actually need them.

Possible future providers include AMP, Multicraft, Pelican, or other common hosting/control systems. Generic HTTP would still cover custom APIs and services that do not need their own adapter.

### Network statistics and history

Keep longer-term network history instead of only current status.

Possible data:

- number of server starts
- total uptime
- peak players
- average startup time
- failed starts
- idle shutdowns
- failed-request shutdowns
- time spent powered off

That could make resource saving measurable instead of just something the owner knows is happening.

```text
Survival
Starts this month: 42
Average startup: 18 seconds
Idle shutdowns: 31
Time powered off: 126 hours
```

## Hub

### Navigator profile editor

Add in-game tools for creating, copying, assigning, and editing Navigator profiles once multi-hub support exists.

That would let an owner build different menus for the main hub, minigame hubs, modded hubs, or other groups without editing YAML by hand.

### Server-group and folder editing

Let nested folders and server groups be created and rearranged from the Hub admin GUI.

That includes slots, icons, titles, parent menus, and which servers or hubs appear inside each group.

### Network preset review

After guided setup builds a preset, show a review screen explaining what the Small, Medium, or Large choice changed before the owner accepts it.

The setup stays fast without hiding what HubPilot configured.

## Interact

### Navigator and profile targets

Let Interact bindings open a Navigator profile or folder instead of only requesting one server.

A Minigames NPC could open the Minigames menu, while a BedWars NPC inside that area could still request BedWars directly.

### More flexible interaction rules

Add more control over who can use a binding, when it is active, and whether it points to a server, hub, group, or Navigator page.

That becomes more useful once multi-hub layouts exist.

## Link

### Multi-hub telemetry sync

Let several Hub servers report their local state to Core while keeping the data inside the HubPilot network.

That would give Core the information it needs for hub groups, load balancing, and failover.

### Hub health reporting

Link could send lightweight hub health such as player count, response state, and assigned Navigator profile so Core can make better routing decisions.

HubPilot telemetry would stay between Core on Velocity and the HubPilot plugins on the hub servers. It would not be sent to an outside analytics or tracking service.

## Versioning and component compatibility

HubPilot keeps one release number across Core, Hub, Interact, and Link.

Core and Hub should normally run the exact same version.

Link and Interact can stay on an older build when a release does not change them and the release notes say the older build is still compatible.

They should not be more than **two published HubPilot releases behind** Core and Hub. If a release says a new Link or Interact build is required, update it even if it is still inside that normal two-release window.

## How roadmap items are chosen

Nothing on this page automatically goes into the next version. Features move forward based on usefulness, implementation risk, testing needs, and feedback from real HubPilot networks.
