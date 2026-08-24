# Release Validation

A build does not count as working just because it compiles. Changed paths are checked as close to the real flow as possible, and anything that still needs live testing is called out instead of being guessed through.

The 3.5.18 release candidate completed eight validation sweeps:

1. JAR/archive and metadata integrity
2. bytecode/control-flow verification
3. Stop Server target mapping
4. Crafty live-discovery behavior
5. Crafty start/stop provider behavior
6. Crafty discovery/provider-ID repair
7. unchanged-path and blast-radius checks
8. decision/regression review

Those artifacts contained 411 classes and 2,753 concrete method bodies, all analyzed successfully.

## 1.0.0

The public 1.0.0 JARs were rebuilt with internal version `1.0.0` and validated as the exact release artifacts. They were not just renamed copies of 3.5.18.

The final JARs passed archive integrity, declared-main-class checks, and JDK ASM BasicVerifier across all 411 classes and 2,753 concrete method bodies.

Final hashes and the full release record are in [`release/1.0.0`](../release/1.0.0/).

## Live-testing scope

Crafty Controller is the only external controller with live field coverage for 1.0.0.

Pterodactyl and Generic HTTP still have controlled implementation testing, but they have not had the same live-network coverage yet. That difference is kept clear in support claims and release notes.
