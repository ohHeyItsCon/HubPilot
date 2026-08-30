# Known Bugs

This page tracks confirmed issues in public HubPilot releases that have not been fixed yet.

## 1.0.1

### Hub server can appear in `/hp discover`

**Status:** Open

Starting with 1.0.1, the configured hub server can appear in `/hp discover` where it was previously ignored.

This is unintended behavior and will be fixed in the next update.

If the hub is added or treated as a normal managed server, it can inherit the global lifecycle defaults. That includes automatic shutdown settings intended for backend game servers.

For a normal HubPilot network, the hub is usually expected to stay online so players always have somewhere to land while other servers start.

#### Current planned fix

The current plan is to expand `/hp setup` with an initial server/world discovery step and ask which discovered server is the hub. The selected hub would be assigned hub-specific lifecycle rules instead of being treated as a normal managed backend.

That same hub-role foundation can later expand into the dedicated Hub Manager and multi-hub system described in the [roadmap](docs/ROADMAP.md).

#### Workaround

Until this is fixed:

- do not add the hub as a normal managed backend through `/hp discover`
- if the hub is already managed, explicitly enable **Always-On server** for it or otherwise disable its automatic shutdown settings
- verify the hub is not inheriting a global idle-shutdown value before leaving the network unattended

The intended behavior is for HubPilot to distinguish configured hub servers from normal backend discovery targets so the hub is not accidentally subjected to backend lifecycle defaults.
