# Deployment readiness — reconstruction follow-up

## Checkpoints and boundaries

The unchanged reconstruction checkpoint is `ea5f439cf2197ccd604921917f4d309fe9e4a5c5`, tagged `checkpoint/reconstruction-public-1.0.2`. The checkpoint is now known to contain a settings-transport reconstruction regression and must not be deployed. Repair commit `7451556` is documented in SETTINGS-RECONSTRUCTION-REPAIR.md. Compatibility commit `127db71` contains only the Paper chain-icon fix and its regression/review material. Subsequent acceptance tooling is not packaged into HubPilot. No lifecycle fixes were included; PROPOSED-FIXES.md remains the separate defect worklist.

No production plugin replacement, production restart, main merge, release or release-asset replacement is authorized by this report. Embedded plugin version strings are still 1.0.2; candidate hashes remain authoritative. Final hashes are in `readiness-artifacts.json`; the older `validation-summary.json` remains checkpoint history.

## Completed automated checks

- Source compilation and package checks pass: all four candidates retain exact baseline entry sets/resources/notices, with no test/platform classes bundled.
- Two builds of the fixed source produce identical JAR hashes.
- All 17 lifecycle characterization cases pass identically against public Core and reconstructed Core. Existing cancellation/retry/stale-demand defects are preserved, not repaired.
- Seven additional settings-transport cases pass against public Core and repaired candidate. The pre-repair candidate failed; these tests reproduce and guard the corrected reconstruction error. The live client retest then applied the actual Hub settings snapshot successfully with no recurrence of the cast error.
- Both historical Hub/Interact fixtures pass against baseline and candidate.
- Four CHAIN regression cases pass: baseline and checkpoint fail on the newer API as expected; fixed menu chooses CHAIN on Paper 1.21.8 and IRON_CHAIN on Paper 1.21.10.
- Actual Paper 1.21.10 startup enables reconstructed Hub, Link and Interact. The disposable probe constructs the real telemetry inventory successfully, including on Crafty's Java 25.0.3 runtime.
- Actual Velocity 4.1.0 loads reconstructed Core. Actual Core provider transport authenticates to a fresh Crafty 4.10.8 fixture, starts real Paper, observes readiness using Velocity's Minecraft ping, and obtains a successful stop response. The backend port is subsequently closed and the isolated Crafty container has zero Java processes, proving process termination. This is not player-admission certification.

See `tests/live/README.md` for the bounded isolated fixture contract, probe building and evidence limitations. All raw deployment inventory, production binaries/configuration, backup data and private logs remain outside GitHub.

## Private installed-delta and recovery work

The installed Hub artifact was compared entry-by-entry and with decompilation plus normalized executable bytecode. Its differences are documented privately, not imported into the source project. Deployment must explicitly reconcile that installed behavior with the public behavioral baseline; matching a version label is insufficient.

A current private backup contains 920 files from the hub and proxy directories, including the installed components, configuration/shared state and three hub worlds. An isolated restored copy matches every recorded file hash. Restored Paper reaches startup readiness and enables the installed HubPilot components; initial test-only native-library restrictions were corrected. An unrelated Essentials compatibility warning remains in the private report.

**Recovery limit:** this was a live file copy, not a quiesced transactional snapshot of every application. Startup/hash equality does not certify arbitrary plugin-database consistency, every chunk, or a full production cutover. A bounded full database-integrity pass did not complete. Preserve this backup, but do not treat it as unconditional deployment approval or off-site disaster recovery.

An offline protocol client entered the actual Paper hub through Velocity with modern forwarding enabled; the expected offline UUID was saved by Paper. A direct backend connection was rejected with the explicit Velocity-required message. This tests protocol/forwarding plumbing, not authenticated account identity.

## Remaining acceptance gates

- Authenticated real-player login, modern-forwarded UUID identity and account authentication.
- Owner claim/roles, GUI command routing, Navigator/Interact triggers, visible/sound countdown feedback, transfer/retry/cancel and admin-item visibility with actual players.
- Forwarding-secret and strict-version rejection cases; modded client/backend handshakes where relevant.
- Reconcile installed component mixture and review the separate compatibility change before any deployment.
- Application-consistent backup strategy and selected plugin-database recovery qualification before cutover.

No claim is made that known lifecycle defects are safe, fixed or field-accepted. No production deployment is performed by any readiness step.
