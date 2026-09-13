# Separate compatibility change: telemetry Link icon

Checkpoint: `ea5f439cf2197ccd604921917f4d309fe9e4a5c5` remains the behavior-preserving reconstruction. This change is a compatibility fix for review, not a lifecycle change or deployment approval.

Both public baseline and checkpoint throw `NoSuchFieldError` when initializing `TelemetryMenuBuilder` with pinned Paper 1.21.10. Paper 1.21.8 succeeds. This affects opening telemetry customization, not necessarily plugin startup: lazy class resolution hides the failure until the class is used.

Fix: resolve `IRON_CHAIN` by name, falling back to `CHAIN` only when the newer material does not exist. No catch-all exception suppression and no unrelated replacement icon. The Link icon retains its chain appearance across both APIs. No dependency upgrade, protocol/resource change, or lifecycle change.

Regression: `python3 reconstruction/chain-test.py --jdk <reference-jdk> --fixed`. Four independent JVM cases: baseline succeeds on 1.21.8 and reproduces the error on 1.21.10; fixed candidate initializes with CHAIN on 1.21.8 and IRON_CHAIN on 1.21.10. The test reads the actual menu's Link icon slot, not an independent helper implementation.

Review scope: one production source file, one private static helper, same icon position/other icons. Existing historical replacement files remain untouched. Public baseline package/ABI checks that intentionally require no extra private members must not be interpreted as product-fix acceptance gates. Full live player menu behavior is a separate acceptance item.
