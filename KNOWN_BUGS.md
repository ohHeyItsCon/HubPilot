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

The September 3 repaired 1.0.2 Core automatically treats the configured `hub-server` and its managed-server identity as a protected hub role on startup and every successful configuration reload. Protection is applied after YAML defaults and shared GUI settings: idle shutdown is disabled, stop-after-failure is false, and stop-when-queue-empty is false. Explicit unsafe overrides cannot re-enable these automatic shutdown paths for the configured hub.

The managed entry is retained. No server files, destinations, provider mappings, or unrelated servers are deleted by this repair. Saved settings and comments remain intact; changing `hub-server` later restores the old entry's configured backend behavior. Provider startup preferences are preserved. Manual Stop Server remains available.

The common automatic-stop dispatcher also checks the current hub identity, protecting sessions created before a reload. `/hp discover` exclusion is unchanged.

The originally published September 1 Core fixed discovery but did not repair already imported hubs. An intermediate local idle-only guard was not in that published artifact and did not cover every shutdown path. Replace Core with the repaired artifact and restart Velocity; no manual Always-On toggle or configuration deletion is required. An existing 1.0.2 Hub can remain installed. When upgrading from 1.0.1, also install the existing 1.0.2 Hub for the Queue Update. Version remains 1.0.2, so identify the repaired build by its supplied SHA-256, not the version label alone.

The larger guided hub-selection flow remains planned for setup and the future Hub Manager described in the [roadmap](docs/ROADMAP.md).

#### Workaround

On stable 1.0.1, until upgrading:

- do not add the hub as a normal managed backend through `/hp discover`
- if the hub is already managed, explicitly enable **Always-On server** for it or otherwise disable its automatic shutdown settings
- verify the hub is not inheriting a global idle-shutdown value before leaving the network unattended

The intended behavior is for HubPilot to distinguish configured hub servers from normal backend discovery targets so the hub is not accidentally subjected to backend lifecycle defaults.
