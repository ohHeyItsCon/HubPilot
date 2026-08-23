# HubPilot 1.0.0 Final Checklist

## Remaining live checks from 3.5.18

- [ ] Stop Server controls the intended backend when a Navigator/destination ID differs from the Velocity backend target.
- [ ] A server deleted from Crafty disappears from `/hp discover` when Crafty is primary, even if Velocity still has an old registration.
- [ ] `/hp discover add *` / `all` imports only servers that still exist in the live Crafty inventory.
- [ ] An offline Crafty-backed request starts the correct backend and completes the queue/transfer flow.

## Documentation and support claims

- [x] Credit the project idea and direction to `ohHeyItsCon`.
- [x] Disclose AI-assisted development.
- [x] State that Crafty Controller is the only external controller covered by live beta testing so far.
- [x] Explain exactly what Always-On does and does not do.
- [x] Explain the recommended always-on hub layout and backend port setup.
- [x] Mark Pterodactyl and Generic HTTP as implemented without claiming live beta coverage.
- [x] Document ViaVersion compatibility without claiming protocol support beyond ViaVersion itself.
- [x] Add an FAQ covering common startup, routing, port, provider, and version problems.

## Final packaging

- [ ] Set all four component versions to `1.0.0` internally.
- [ ] Build `HubPilot-Core-1.0.0.jar`.
- [ ] Build `HubPilot-Hub-1.0.0.jar`.
- [ ] Build `HubPilot-Interact-1.0.0.jar`.
- [ ] Build `HubPilot-Link-1.0.0.jar`.
- [ ] Check all four JAR archives.
- [ ] Confirm each declared main class exists.
- [ ] Run bytecode/control-flow verification against the exact final JARs.
- [ ] Re-run changed-path integration/runtime tests against those exact JARs.
- [ ] Scan the final release for development usernames, domains, paths, old plugin names, stale versions, credentials, and test-only classes.
- [ ] Record SHA-256 for all four final JARs.
- [ ] Choose and add the project license.
- [ ] Change the README release status from candidate to stable.
- [ ] Put final hashes in the release notes.
- [ ] Tag `v1.0.0` and attach the four JARs to the GitHub Release.
