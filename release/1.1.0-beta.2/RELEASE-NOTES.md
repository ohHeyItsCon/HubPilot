# HubPilot 1.1.0-beta.2

A cleanup beta. Behaviour is meant to match 1.1.0-beta.1; nothing new for players or admins.

## Changes
- Core: `HubPilotReflect` is merged into `Reflect`, with one shared boxing helper.
- Hub: removed two dead classes (`ProtectedItemListener`, never registered, and the unused `PublicGuiEnhancer`).
- Hub: the legacy destination menu builder is folded into `DestinationMenuBuilder`.
- Build: the lifecycle and settings test runners read the jar name from `VERSION` instead of a hard-coded version.

## Checked
- Built, packaged and tested by the GitHub Actions beta workflow: packaging checks, the 17 lifecycle and 7 settings differential cases against the published 1.0.2 baseline, and a byte-for-byte reproducibility rebuild. The release jars are that workflow's output.
- The same suites matched the beta.1 baseline locally before the version bump.

## Not checked
- No live install on the Crafty hub or Velocity, and no real-player test. Both servers are still stopped.

## Install
Replace all four jars together and compare them against `CHECKSUMS-SHA256.txt`.
