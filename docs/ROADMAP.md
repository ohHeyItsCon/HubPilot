# HubPilot Roadmap

This page lists ideas being considered for future HubPilot versions. Nothing here is promised for a specific release until it is moved into an actual release plan.

## Multi-hub and Navigator profiles

Allow different hubs to use different Navigator layouts and server groups.

A main hub could send players to a Minigames Hub, Survival Hub, or Modded Hub, while each sub-hub has its own Navigator for the servers that belong to it.

This would also make it possible for multiple physical hub servers to share the same Navigator profile.

## Nested Navigator folders

Allow Navigator entries to open another Navigator page instead of always pointing directly to a server.

A smaller network could keep one hub but organize servers into categories such as Survival, Minigames, Modded, or Events. The same system could also be used by larger multi-hub networks.

## Hub groups and load balancing

Allow several hub servers to be treated as one logical hub group.

For example, `hub-1`, `hub-2`, and `hub-3` could all use the same Navigator profile, and HubPilot could choose the healthiest or least-populated hub when a player uses `/hub` or needs a fallback destination.

## Hub failover

Allow backup hubs to take over automatically when the preferred hub is offline or unreachable.

This would keep `/hub`, `/lobby`, and other return-to-hub flows working even if one hub server goes down.

## Always-on network layouts

Add setup presets for networks that intentionally keep some or all game servers running at all times.

HubPilot already has the Always-On provider. This roadmap item is about making the setup flow understand larger networks that have enough hardware to keep multiple hubs, worlds, minigames, or backend servers online permanently instead of assuming resource-saving shutdown is always wanted.

## Setup profiles and guided defaults

Expand `/hp setup` so it can ask a few questions about the network before generating defaults.

Possible questions include:

- Is the network small, medium, or large?
- Is there one hub or several hubs?
- Should backend servers stay online or start only when requested?
- Which power provider is being used?
- Are most servers lightweight, heavily modded, or mixed?
- Should idle shutdown be enabled by default?
- How long do servers normally take to start?

HubPilot could then load sensible presets instead of making every owner configure the same basic settings by hand.

A small network preset could favor simple navigation and aggressive resource saving. A medium preset could use longer timeouts and more flexible server groups. A large or always-on preset could disable automatic shutdown by default and prepare the network for multiple hubs and larger queues.

The selected profile would only provide starting defaults. Every setting would still be editable afterward.

## Better queue controls

Expand the current offline-server request flow into a more complete queue system.

Possible additions include queue position, estimated wait time, maximum queue size, canceling a request, staff or VIP priority, and better handling when many players request the same offline server at once.

## Scheduled server availability

Allow servers to become available or unavailable automatically on a schedule.

This could be useful for event servers, seasonal worlds, weekend-only servers, maintenance windows, or networks that only want certain servers powered during specific hours.

## More server-management providers

Add direct integrations for other commonly used panels when there is enough demand.

Possible future providers could include AMP, Multicraft, Pelican, or other hosting/control systems. Generic HTTP would remain available for systems that do not need a dedicated adapter.

## Network statistics and history

Expand HubPilot statistics into longer-term network history.

Possible data could include server starts, uptime, peak players, average startup time, failed starts, idle shutdowns, and how long provider-controlled servers remained powered off.

This could help owners see which servers actually get used and how much runtime HubPilot is saving.

## How roadmap items are chosen

Roadmap ideas are not automatically assigned to the next version. Features will be prioritized based on usefulness, implementation risk, testing requirements, and feedback from real HubPilot networks.
