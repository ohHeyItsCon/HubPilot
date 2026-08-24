# HubPilot 1.0.0 Final Checklist

## Live testing record

The 3.5.18 validation report listed a few deployment-specific live checks that were not separately recorded as completed. The final 1.0.0 artifacts still went through the release version, archive, metadata, and bytecode checks before publication.

## Documentation and support claims

- [x] Credit the project idea and direction to `ohHeyItsCon`.
- [x] Disclose AI-assisted development.
- [x] State that Crafty Controller is the only external controller with live beta coverage so far.
- [x] Explain what Always-On does and does not do.
- [x] Explain the recommended always-on hub layout and backend port setup.
- [x] Mark Pterodactyl and Generic HTTP as implemented without claiming live beta coverage.
- [x] Document ViaVersion compatibility without claiming protocol support beyond ViaVersion itself.
- [x] Document LuckPerms compatibility for hub-side permission nodes.
- [x] Add provider API setup instructions.
- [x] Add an FAQ for common startup, routing, port, provider, and version problems.

## Final packaging

- [x] Set all four component versions to `1.0.0` internally.
- [x] Build `HubPilot-Core-1.0.0.jar`.
- [x] Build `HubPilot-Hub-1.0.0.jar`.
- [x] Build `HubPilot-Interact-1.0.0.jar`.
- [x] Build `HubPilot-Link-1.0.0.jar`.
- [x] Check all four JAR archives.
- [x] Confirm each declared main class exists.
- [x] Run bytecode/control-flow verification against the final JARs.
- [x] Confirm release-code deltas against the validated 3.5.18 artifacts.
- [x] Scan the release for private usernames, domains, paths, credentials, stale versions, and test-only classes.
- [x] Record SHA-256 for all four JARs and the release ZIP.
- [x] Add the MIT license and AutoServer attribution.
- [x] Mark the README release status as stable.
- [x] Put final hashes in the release notes.
- [x] Create the `v1.0.0` GitHub tag and GitHub Release.
- [x] Attach the four JARs and release ZIP to the GitHub Release.
