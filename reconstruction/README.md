# HubPilot 1.0.2 public-source reconstruction

This is a **source-only, four-component reconstruction candidate**, not the original build workspace and not a new approved release. It is based exclusively on the hash-pinned public 1.0.2 JARs in `development/handoff-2026-09-13/public-artifacts.json`, plus the nine surviving replacement source files. No production or pre-public workspace is an input.

## Readiness follow-up

See [DEPLOYMENT-READINESS.md](DEPLOYMENT-READINESS.md) for subsequent live acceptance. The preserved `ea5f439` checkpoint has a discovered settings-transport reconstruction regression; [SETTINGS-RECONSTRUCTION-REPAIR.md](SETTINGS-RECONSTRUCTION-REPAIR.md) documents its bytecode-backed correction. [CHAIN-REVIEW.md](CHAIN-REVIEW.md) is a separate compatibility fix. Neither is a deployment approval.

## Build

Requirements: Python 3.10+, a JDK (not just JRE), HTTPS access for the first dependency download. Reference toolchain is **Eclipse Temurin 21.0.8+9, Linux x64**. All own classes are compiled with `--release 21 -proc:none -g -encoding UTF-8`. Other platforms/JDK builds have not been qualified for identical output.

From the repository root:

```sh
# Optional: materialize the exact pinned JDK in ignored build/toolchain (no host install).
python3 reconstruction/bootstrap.py

# Compile all four components and package candidates. No HubPilot baseline classes on javac's classpath.
python3 reconstruction/build.py --jdk build/toolchain/jdk-21.0.8+9

# Complete reference verification: recover/check public baseline, source build, packages,
# ABI, class loading, bytecode comparison, lifecycle differential tests, retained fixtures,
# and a second clean-output build with exact JAR comparison.
python3 reconstruction/verify.py --jdk build/toolchain/jdk-21.0.8+9 --fixtures

# After baseline and dependencies have been cached, repeat without network downloads.
python3 reconstruction/verify.py --jdk build/toolchain/jdk-21.0.8+9 --fixtures --offline
```

An existing matching JDK may be passed through `--jdk`; the compiler checks version 21.0.8. The bootstrap lock records the full reference distribution hash/vendor. `bootstrap.py` refuses to overwrite an existing extracted JDK. Build outputs are under `build/candidate`, separately named `HubPilot-<Component>-1.0.2-reconstructed.jar`. Their embedded baseline plugin descriptors remain 1.0.2 intentionally; identify candidates using filename **and** the generated build manifest/hash, never just the embedded version. **Do not install these as an update.**

## Project layout and dependencies

- `core/src/main/java`, `hub/src/main/java`, `link/src/main/java`, `interact/src/main/java`: all own implementation sources, including binary-era helper classes.
- Each component's `src/main/resources`: exact packaged public resources and license notices.
- `development/1.0.2`: unchanged historical replacement sources, resources and two fixtures. Their nine implementation files are copied byte-for-byte into the new source layout and checked against the originals.
- `reconstruction/dependencies.lock.json`: exact coordinate, timestamped snapshot URL, SHA-256 and scope of every compile/inspection/lifecycle dependency. No transitive dependency discovery at build time.
- `reconstruction/fixtures.lock.json`: separate pinned MockBukkit/Paper test runtime additions.
- `reconstruction/source-provenance.json`: initial recovered source hashes and original replacement-file origin paths.
- `reconstruction/toolchain.lock.json`: JDK and both decompilers used for recovery. Decompilers are **not** needed to build committed source.
- `tests/src`: new baseline-characterization tests. `tests/inspection`: metadata and executable-bytecode inspectors.

The build uses direct `javac` plus standard-library deterministic JAR packaging, not a partially specified Maven/Gradle resolver. There is one authoritative dependency lock and one packaging implementation. It compiles the four source roots together to resolve Hub/Interact cross-references, then packages only each component's own namespace. Hub/Interact declarations and descriptors preserve their original runtime dependency relationship.

Reference compile APIs are Velocity 3.4.0 snapshot **20260121.190037-118** and Paper 1.21.8 snapshot **20250906.215025-55**. These are newly selected reconstruction inputs, **not claimed recovered historical coordinates**. Paper 1.21.8 retains `Material.CHAIN`, which the baseline Hub references. The retained MockBukkit fixtures use Paper 1.21.10 snapshot **20260104.211118-51** separately. Preserving that reference does not solve its version-sensitive runtime linkage on newer Paper; see DIFFERENCES.md. Runtime Java must be 21-capable; production Java versions are not used for development.

Only **SnakeYAML 2.1** is bundled, in Core's original unrelocated namespace. Paper, Velocity, Adventure, Guice, ASM, MockBukkit, fixtures and test classes are never packaged. All public baseline non-class resources are preserved, including HubPilot MIT, Artificial-720/AutoServer MIT, SnakeYAML license/NOTICE and embedded dependency metadata. Root LICENSE and THIRD_PARTY_NOTICES.md remain unchanged. Recovered Java is labeled as reconstruction, not original authorship.

## Verification and evidence boundaries

`verify.py` stops on a failed prerequisite. Verification reports and logs remain in ignored `build/`. Package checks require exact baseline file-entry sets, identical resources, correct component namespaces and no test/platform classes. ABI checks cover all 201 own class names, named field/method descriptors and inheritance. Java `-Xverify:all` class loading/member reflection is checked independently on baseline and candidate, **without plugin initialization**. This is not proof that every lazily resolved API call or reflective path will succeed.

Baseline SnakeYAML was transformed during historical packaging. Maven 2.1 has different bytes for 128 of 229 class entries, but normalized executable instructions, constants, target locations, exception-handler order and member flags are identical for all 229. Stack-map/constant-pool/debug encoding is not compared as behavior. The candidate uses the locked upstream dependency, not classes copied out of Core.

The same lifecycle harness runs in separate JVMs against public Core and reconstructed Core. Real implementation includes config loading, effective rules, shared sessions, queues, connection/retry logic and provider transport. Velocity players/servers/scheduler are controlled interfaces; provider calls go only to an ephemeral loopback HTTP fixture. Tests use a deterministic released-task scheduler and bounded asynchronous waits; no production endpoint, credentials, panel or real game process. Tests explicitly retain undesirable baseline observations. See PROPOSED-FIXES.md for changes that **are not implemented here**.

Two independent fresh-output builds must produce identical JAR bytes. This demonstrates reproducibility for the pinned reference environment; it does not mean the reconstructed JARs match the historical binary hashes or prove complete semantic equivalence. See RESULTS.md for measured checks and remaining gaps.

## Maintenance boundary

Keep recovery/compilation corrections separate from product fixes. Preserve the published releases, tags and installation. A future change needs its own tests, immutable build identity and approval appropriate to deployment/release. Crafty-first **Prepare for Velocity** remains the next committed major product direction; reconstruction does not introduce Controller integration or replace Core's lifecycle authority. Public-source reconstruction contains no private inventory; that report is stored separately outside this repository.
