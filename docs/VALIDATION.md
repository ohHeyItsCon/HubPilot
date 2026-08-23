# Release Validation

A build is not treated as working just because it compiles. Changed paths are checked as close to the real flow as practical, and anything that still needs a live network test stays marked as unfinished.

The 3.5.18 release candidate completed eight validation sweeps:

1. JAR/archive and metadata integrity
2. bytecode/control-flow verification
3. Stop Server target mapping
4. Crafty live-discovery behavior
5. Crafty start/stop provider behavior
6. Crafty discovery/provider-ID repair
7. unchanged-path and blast-radius checks
8. decision/regression review

The packaged 3.5.18 artifacts contained 411 classes, with 2,753 concrete method bodies analyzed successfully.

## Live-testing scope

The beta network uses Crafty Controller, so Crafty has live field coverage that Pterodactyl and Generic HTTP do not currently have.

Lab and harness validation still matter, but they are kept separate from live controller testing when support claims are written.

## 1.0.0 release rule

The 1.0.0 release cannot be made by only renaming the 3.5.18 files. All four JARs need internal version `1.0.0`, and validation must run against those exact final artifacts.

Final SHA-256 hashes will be published with the release.
