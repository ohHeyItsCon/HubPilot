# HubPilot 1.0.0 Final Checklist

## Live testing record

The 3.5.18 validation report listed several deployment-specific live acceptance checks that were not separately recorded as completed in the release record. The maintainer approved the public 1.0.0 release after the final artifact rebase and validation.

## Documentation and support claims

- [x] Credit the project idea and direction to `ohHeyItsCon`.
- [x] Disclose AI-assisted development.
- [x] State that Crafty Controller is the only external controller covered by live beta testing so far.
- [x] Explain exactly what Always-On does and does not do.
- [x] Explain the recommended always-on hub layout and backend port setup.
- [x] Mark Pterodactyl and Generic HTTP as implemented without claiming live beta coverage.
- [x] Document ViaVersion compatibility without claiming protocol support beyond ViaVersion itself.
- [x] Document LuckPerms compatibility for hub-side permission nodes.
- [x] Add full provider API setup instructions.
- [x] Add an FAQ covering common startup, routing, port, provider, and version problems.

## Final packaging

- [x] Set all four component versions to `1.0.0` internally.
- [x] Build `HubPilot-Core-1.0.0.jar`.
- [x] Build `HubPilot-Hub-1.0.0.jar`.
- [x] Build `HubPilot-Interact-1.0.0.jar`.
- [x] Build `HubPilot-Link-1.0.0.jar`.
- [x] Check all four JAR archives.
- [x] Confirm each declared main class exists.
- [x] Run bytecode/control-flow verification against the exact final JARs.
- [x] Confirm release-code deltas against the validated 3.5.18 artifacts.
- [x] Scan the final release for private usernames, domains, paths, credentials, stale versions, and test-only classes.
- [x] Record SHA-256 for all four final JARs and the release ZIP.
- [x] Add the MIT license and AutoServer attribution.
- [x] Change the README release status to stable.
- [x] Put final hashes in the release notes.
- [ ] Create the `v1.0.0` GitHub tag and GitHub Release.
- [ ] Attach the four JARs, ZIP, and validation report to the GitHub Release.
