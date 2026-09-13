# Reconstruction validation — 2026-09-13

## Established checks

- Four-component source build: **PASS**, 142 Java source files, no baseline HubPilot JAR on compilation classpath or implementation-class overlay.
- All nine surviving replacement implementation files: **unchanged**, exact copied bytes verified.
- Packaging: **PASS**, exact original entry sets (including all 201 own classes), identical non-class resources/notices, only intended component namespace plus locked Core SnakeYAML; no platform/test classes.
- Named own-class members/descriptors, relevant access flags and inheritance: **PASS**, no missing named members across 201 classes. Compiler/synthetic metadata differences are documented separately.
- JVM `-Xverify:all` class loading and member reflection: **PASS** for all 201 own classes on both public baseline and reconstructed candidates. No plugin initialization.
- SnakeYAML normalized executable comparison: **PASS** for all 229 classes; candidate bundles upstream locked 2.1, with historical packaging-byte differences explained.
- Core differential characterization: **17/17 PASS on public baseline and 17/17 PASS on reconstruction**, identical observations. Includes eight simultaneous shared requesters, control dispatch, auth-header switches, cancellation, delayed retries, stale maintenance/Always-On, competing demand, idle/connected/configured-hub protection and outstanding provider start.
- Two fresh-output builds in the pinned environment: **byte-identical for all four JARs**. A clean Git export of implementation checkpoint `e4aca13` with an empty dependency cache and no baseline JARs also produced exactly the same four artifacts.

- Retained `InteractValidation`: **PASS on baseline and candidate**, unchanged source; portal persistence/styles, brush/editor flow, permissions/item safety and label lifecycle.
- Retained `HubToggleValidation`: **PASS on baseline and candidate**, unchanged source, Interact absent; Core-message-to-Hub route, admin-item toggling/persistence, guest denial and inventory safety.
- These retained fixtures use a newly locked MockBukkit 4.95.0/Paper 1.21.10 dependency environment, not a claimed recovered historical lock. Mocked display styling and partial plugin setup retain the fixtures’ original limitations.

Actual commands completed: `verify.py --jdk <pinned-jdk> --offline`, `fixtures.py --jdk <pinned-jdk>`, and `build.py --jdk <pinned-jdk>` from a clean committed-source export with fresh dependency downloads. The documented `--fixtures` option composes the same retained-fixture runner into the full verifier.

## Candidate identities

These are candidate hashes, not public release replacements:

| Component | SHA-256 |
|---|---|
| Core | `11782743ad875cbcea81f195f0af0874e50e61ff14deeab2f251f3320a04c848` |
| Hub | `2457d2e845fe7fa6482b0ab6c5c65906016d697b07531b5af0686e94b3686bfb` |
| Link | `bc4e25c64192df4b112832bacb69f6ec569aabedd867b4ed0c7c89799b0359b9` |
| Interact | `f5ba407f3bfd175b2e40210a72dcc38897a557d9aa8073d77bd98e902eb05c96` |

## Evidence

Run commands in README.md. Generated evidence is under `build/candidate/{build-manifest,packaging-verification,abi-comparison,dependency-bytecode-verification,reproducibility}.json`, plus `build/{baseline,reconstructed}-lifecycle.log`, linkage logs, and retained fixture logs. These generated artifacts are not committed as source or published as release assets.

## Limits and blockers to production acceptance

No compilation blocker remains for the pinned reconstruction environment. This does not recover the original historical toolchain or prove arbitrary JDK/platform reproducibility. Reconstructed class hashes differ from the public baseline by design; whole-suite semantic equivalence is not claimed.

The baseline's Paper `Material.CHAIN` reference is version-sensitive and not fixed here. Full plugin initialization, actual Velocity/Paper player acceptance, forwarding/mod handshakes, real providers, complete malicious-input/concurrency/load campaigns and production restore are **not run**. Read-only private inventory and preservation checks are stored outside this public repository. They do not constitute deployment approval or verified backup/restore readiness.

No production deployment, published release/tag replacement or lifecycle fix has been performed. Reproduced undesirable baseline behaviors are separated in PROPOSED-FIXES.md.
