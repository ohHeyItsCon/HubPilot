# Reconstruction differences and bytecode decisions

Baseline: public repaired 1.0.2 assets pinned in the handoff manifest. Historical source tree baseline `1640ab09460bd0f34c86533bc229de3283689b1a`; reconstruction branched from public handoff commit `a684687558a4a3b43c59a5d500ee3908ee4c3806`.

## Recovery method

CFR 0.152 initially recovered the suite but produced malformed locals, scope errors and ambiguous finally control flow. Vineflower 1.11.1 recovered cleaner control flow; committed reconstructed files use that output except the nine untouched surviving replacement sources. `source-provenance.json` identifies both sources and initial hashes. Source-level restoration is not a proof of the original local naming/style or historical build flags. Many compiler-erased local names remain `varN`; readability improvements can be made in separately reviewed refactors.

Direct `javap -p -c -s` was taken from each public component for ambiguous methods. It is reproducible with the pinned JDK, e.g.:

```sh
build/toolchain/jdk-21.0.8+9/bin/javap -classpath build/baseline/HubPilot-Core-1.0.2.jar -p -c -s dev.hubpilot.core.PublicControlGate dev.hubpilot.core.ProviderRegistry dev.hubpilot.core.LifecycleManager
```

## Material decisions (compilation restoration, not bug fixes)

| Area | Ambiguity and resolution |
|---|---|
| `PublicControlGate.dispatch` | CFR's finally-return-false is wrong. Actual offsets 249/250 return true after successful reflective dispatch; separate exception handlers return false. Vineflower recovers that shape. Differential test explicitly covers recognized/rejected opcodes and unsupported input. |
| `ServerAdminBridge.invokeOn`, `ServerControlUi.call` | CFR merged the current `Class` with a method-array local. Bytecode walks `getDeclaredMethods`, then `getSuperclass`, then public-method fallback. Vineflower recovers separate locals, retaining the same search order. |
| `ProviderRegistry.request` | CFR emitted void/uninitialized discriminants. Actual switch assigns provider/auth case integers; recovered string switches retain bearer defaults for Crafty/Pterodactyl and explicit bearer/basic/x-api-key/none. Differential header tests exercise both defaults and overrides. |
| `HubPilotCommands.registerStatusBridge` | Vineflower failed its InvocationHandler lambda. Reconstructed from direct bytecode: Object toString/hashCode/identity-equals, execute event dispatch, otherwise default return value. No added authorization or dispatch path. |
| `PublicCommandLayer.findOnline` | Baseline constructs an Object array containing one null before `Reflect.call(...,"orElse", ...)`. Recovered bare varargs null would pass a null array. Explicit `(Object)null` preserves the one-argument bytecode contract. |
| `PublicHubBootstrap.chat` | Bytecode captures a finalized message string after optional prefixing; Java source needs a final local. Its delayed setup lambda captures `event.getPlayer()`, not the event itself. Both restored without changing the delayed action. |
| `LifecycleManager.sendCountdownViaHub` | Restored parentheses around a Boolean assignment inside OR. Preserves null/non-Boolean acceptance and actual Boolean return, not unconditional success. |
| Generic locals | Restored typed maps/collections/streams from use sites, method signatures and emitted casts, including config/discovery, permissions/provider definitions, destination parsing/layout and NPC collections. Method descriptor/access checks catch lost named binary members; generics metadata can legitimately differ. |
| Legacy destination default slot | Recovered `byte` would introduce narrowing during `+= 2`; baseline uses integer increment. Kept an int. |
| Legacy enum switch helper | The public `LegacyDestinationMenuBuilder` still uses `DestinationMenuBuilder$1`'s ordinal map. Explicitly retained and referenced that helper instead of emitting a differently named extra class. |
| Orphaned historical helper classes | `ClaimTimeout$1` and `CraftyProvider$1` survive in public Core; retained as explicit source classes, including the latter's captured outer reference. They are not dropped as obsolete. |

## Intentional candidate differences

- All own classes are recompiled as Java 21 major 65 instead of the original mixture of 61 and 65.
- Class bytes differ because of compiler/debug/local/generic/constant-pool/lambda metadata; JAR ordering/timestamps/compression are deterministic (sorted entries, 1980 timestamp, stored entries). The artifact is not a bit-for-bit recreation of the original release. Embedded `Created-By` manifest values remain baseline resource bytes, not a statement of this build’s compiler; the generated build manifest records the actual compiler.
- Named own-class members, descriptors, relevant access flags and inheritance are checked. Compiler-generated lambda names and synthetic/enclosing metadata can differ; exact anonymous-class reflection metadata is not claimed recovered. In particular, explicit retained `$1` helpers expose formerly synthetic fields as ordinary source fields, and the switch helper gains javac’s default constructor. These are recorded metadata differences, not dropped classes. No cross-component named method is intentionally removed or renamed.
- SnakeYAML 2.1 is locked upstream bytes; all 229 classes have verified normalized executable equivalence to the packaged baseline. Its 128 byte-different entries are packaging transformation differences, not an upgrade.
- Candidate filenames carry `-reconstructed`; public embedded descriptors/resources are unchanged. The generated hash/build manifest is the candidate identity.
- Compile dependency versions are newly pinned reconstruction choices. Historical dependency/JDK lock remains unknown. Build does not resolve snapshots by floating metadata.

## Known API and validation limitations

The baseline Hub's `TelemetryMenuBuilder` references `Material.CHAIN`; the selected Paper 1.21.10 API lacks that field. Compilation uses pinned Paper 1.21.8 to preserve the original reference. A modern-Paper compatibility fix must be explicit and tested on the actual platform; changing to another material here would silently change behavior. Class loading without initialization cannot prove this static initializer works on Paper 1.21.10.

No actual Velocity/Paper startup, real player login, mod/forwarding handshake, public-provider compatibility run, full security audit, sustained load or production restore is claimed. Characterization of concrete isolated failure interleavings is not a report that a player suffered them in production. Config/discovery/UI reflective paths not covered by tests remain review targets, not implicitly certified.
