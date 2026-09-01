# Known Bugs

This page tracks confirmed issues in public HubPilot releases and where their fixes are available.

## 1.0.1

### Hub server can appear in `/hp discover`

**Status:** Fixed in the 1.0.2 pre-release

Starting with 1.0.1, the configured hub server can appear in `/hp discover` where it was previously ignored.

This is unintended behavior. HubPilot 1.0.2 excludes the configured hub from discovery lists, suggestions, bulk discovery, and the final import path. The comparison also covers common equivalent hub names so an API-reported alias is not imported as a backend.

If the hub is added or treated as a normal managed server, it can inherit the global lifecycle defaults. That includes automatic shutdown settings intended for backend game servers.

For a normal HubPilot network, the hub is usually expected to stay online so players always have somewhere to land while other servers start.

#### Fix

The immediate regression is fixed in 1.0.2 without changing existing configuration. The configured `hub-server` and its managed-server identity are treated as a protected hub role during discovery.

The larger guided hub-selection flow remains planned for setup and the future Hub Manager described in the [roadmap](docs/ROADMAP.md).

#### Workaround

On stable 1.0.1, until upgrading:

- do not add the hub as a normal managed backend through `/hp discover`
- if the hub is already managed, explicitly enable **Always-On server** for it or otherwise disable its automatic shutdown settings
- verify the hub is not inheriting a global idle-shutdown value before leaving the network unattended

The intended behavior is for HubPilot to distinguish configured hub servers from normal backend discovery targets so the hub is not accidentally subjected to backend lifecycle defaults.
