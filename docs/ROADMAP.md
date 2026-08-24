# HubPilot Roadmap

This page lists ideas being considered for future HubPilot versions. Nothing here is promised for a specific release until it is moved into an actual release plan.

## Multi-hub and Navigator profiles

Allow different hubs to use different Navigator layouts and server groups.

A main hub could send players to a Minigames Hub, Survival Hub, or Modded Hub, while each sub-hub has its own Navigator for the servers that belong to it.

For example:

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

Multiple physical hub servers could also share the same Navigator profile. This would let a larger network run several copies of one hub without rebuilding the same menu on every server.

## Nested Navigator folders

Allow Navigator entries to open another Navigator page instead of always pointing directly to a server.

A smaller network could keep one hub but organize servers into categories such as Survival, Minigames, Modded, or Events.

For example:

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

The same Navigator-profile system could power both nested menus and physical sub-hubs so they do not need to be built as two completely separate features.

## Hub groups and load balancing

Allow several hub servers to be treated as one logical hub group.

For example, `hub-1`, `hub-2`, and `hub-3` could all use the same Navigator profile. HubPilot could choose the healthiest or least-populated hub when a player uses `/hub`, `/lobby`, or needs a fallback destination.

This would make HubPilot more useful on networks that need more than one hub instance to handle player load.

## Hub failover

Allow backup hubs to take over automatically when the preferred hub is offline or unreachable.

If `hub-1` goes down, HubPilot could route returning players to `hub-2` instead of leaving `/hub`, `/lobby`, or another return-to-hub flow broken.

Failover could work alongside hub groups so large networks can have both load balancing and backup destinations.

## Per-server operating modes

Add an operating mode for each managed server so HubPilot knows how that server is supposed to behave after it has been configured.

This would be separate from the provider. The provider answers **how HubPilot controls the process**. The operating mode answers **how HubPilot should treat the server once that control is available**.

Possible modes:

### Always On

The server is expected to stay running all the time.

HubPilot would not use idle shutdown or failed-request shutdown against that server. This is useful for larger networks that have enough hardware to keep popular worlds, minigames, or hub servers running continuously.

An Always On server could still use Crafty, Pterodactyl, Generic HTTP, or another power provider if the owner wants HubPilot to be able to start it again after a crash or manual stop.

### On Demand

The server is allowed to stay offline until somebody requests it.

HubPilot can start it, wait for it to become reachable, transfer players, and shut it down again later according to the configured idle or failed-request rules.

This would remain the best fit for smaller networks or expensive backend servers that do not need to run all day.

### Custom

The owner chooses the lifecycle behavior manually.

This could allow any combination of automatic startup, idle shutdown, failed-request shutdown, queue behavior, retry settings, and timeout values without forcing the server into one of the preset modes.

A mixed network could then look like:

```text
Main Hub       -> Always On
Minigames Hub  -> Always On
BedWars        -> Always On
SkyWars        -> Always On
Events         -> On Demand
Modded SMP     -> On Demand
Seasonal       -> On Demand
```

## Guided setup and network-size presets

Expand `/hp setup` so it asks a few questions about the network before generating the first set of defaults.

The goal would be to get a new installation close to a sensible configuration without making the owner manually tune every timeout, shutdown rule, queue setting, and Navigator option before the network is usable.

Possible setup questions:

- Is the network small, medium, or large?
- Is there one hub or more than one hub?
- Should most servers be Always On, On Demand, or a mix of both?
- Which power provider manages the servers?
- Are the backends mostly lightweight Paper servers, heavily modded servers, or mixed?
- Roughly how long do the servers normally take to start?
- Should idle shutdown be enabled by default?
- Does the network need multiple Navigator groups or only one main Navigator?

HubPilot could use those answers to load a starting profile.

### Small network preset

A small preset could assume one hub, a simple Navigator, mostly On Demand backends, shorter idle timers, smaller queues, and more aggressive resource saving.

### Medium network preset

A medium preset could assume a mix of Always On and On Demand servers, longer startup timeouts, larger queues, server categories, and room for a second hub or more complex Navigator layout.

### Large network preset

A large preset could assume several hubs, many Always On servers, larger queues, longer or server-specific timeouts, hub groups, failover options, and little or no automatic shutdown unless the owner chooses it.

The selected size would only load starting defaults. It would never permanently lock settings or prevent a small network from using advanced features.

## Better queue controls

Expand the current offline-server request flow into a more complete queue system.

Possible additions include:

- queue position
- estimated wait time
- maximum queue size
- canceling a request
- staff or VIP priority
- better handling when many players request the same offline server at once
- clearer behavior when a server fails to start

This becomes more useful as HubPilot is used on larger networks where several players may request the same backend at the same time.

## Scheduled server availability

Allow servers to become available or unavailable automatically on a schedule.

This could be useful for event servers, seasonal worlds, weekend-only servers, maintenance windows, or networks that only want certain servers available during specific hours.

A scheduled server could be hidden from the Navigator, shown as unavailable, or prevented from starting outside the configured schedule.

## More server-management providers

Add direct integrations for other commonly used panels when there is enough demand.

Possible future providers could include AMP, Multicraft, Pelican, or other hosting/control systems. Generic HTTP would remain available for systems that do not need a dedicated adapter.

New providers should be added when there is a real use case instead of increasing Core size with integrations nobody is using.

## Network statistics and history

Expand HubPilot statistics into longer-term network history.

Possible data could include:

- number of server starts
- total uptime
- peak players
- average startup time
- failed starts
- idle shutdowns
- failed-request shutdowns
- time spent powered off

This could help owners see which servers actually get used and how much runtime HubPilot is saving.

A future admin view could show something like:

```text
Survival
Starts this month: 42
Average startup: 18 seconds
Idle shutdowns: 31
Time powered off: 126 hours
```

## How roadmap items are chosen

Roadmap ideas are not automatically assigned to the next version. Features will be prioritized based on usefulness, implementation risk, testing requirements, and feedback from real HubPilot networks.
