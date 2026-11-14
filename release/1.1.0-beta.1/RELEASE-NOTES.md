# HubPilot 1.1.0-beta.1 — prepared beta assets

Not published or deployed. Crafty and Velocity are unchanged.

## Integration
- Core adds the HubPilot Controller provider with owner-approved pairing, certificate pinning and per-request idempotency keys.
- Core adds protocol-1 pairing, heartbeats and transfer reporting; completion follows Velocity confirmation.

## Build cleanup
- All four plugins use deterministic deflated JARs rather than uncompressed archives.
- One root VERSION generates per-component constants for runtime messages and protocol announcements; descriptors are synchronized during packaging.
- Core upgrades SnakeYAML 2.1 to 2.7 and relocates it to dev.hubpilot.libs.snakeyaml, avoiding Velocity library collisions. Dependency licenses and Maven metadata are retained.
- Added beta packaging checks and a GitHub Actions build/regression/reproducibility workflow. Workflow has not run on GitHub yet.
- JARs are ignored by git; these local files are intended as GitHub Release attachments.

## Verified locally
- Four artifact packaging checks (namespaces, compression, descriptors, dependency metadata, integrity).
- 17 differential lifecycle cases and 7 differential settings cases match the published 1.0.2 baseline.
- Two fresh output builds produce byte-identical JARs.
- Candidate lifecycle suite also runs without an external SnakeYAML dependency.

## Remaining boundaries
- No real player/network acceptance or live server installation in this cleanup pass.
- Historical reconstruction verify.py is not the beta gate (it enforces the old package inventory).
- Larger overlay removal, self-reflection cleanup and decompiler-variable renaming remain staged work. No whole-codebase rewrite is claimed.
- Pinned javac/Python build remains authoritative; Gradle migration is deferred to avoid two competing build implementations.
