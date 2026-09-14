# Isolated live acceptance probes

These are disposable test plugins, not suite components. Never put them in production.
Build using `python3 reconstruction/live-probes.py --jdk <reference-jdk>` after the normal build has populated pinned dependencies.

## Measured platform matrix

- Paper 1.21.10-130-8043efd and Minecraft protocol 773.
- Velocity 4.1.0-SNAPSHOT git-00759e52-b16.
- Crafty 4.10.8, using a fresh registry and the actual installed image bytes in isolation.
- Temurin 25.0.4+7 for the isolated proxy/Paper process; Crafty's Ubuntu Java 25.0.3+9 for the provider-managed backend.
- Suite candidate: Java-21-target source build; the separately committed CHAIN fix plus the bytecode-backed settings reconstruction repair differ from the preserved checkpoint.

Platform binary provenance and deployment-specific details are private. Public source/release assets and production files are not changed by these tests.

## Fixture contract

Use no production mounts, provider credentials, owners, server IDs or network routes. The measured fixture uses one resource-bounded, network-disabled server container and a fresh Crafty container sharing that isolated network namespace. No ports are published. Preserve raw logs privately. The first native-library run required an executable temporary directory for JNA/SQLite; this is a lab setup requirement, not a product fix.

- Proxy listener: loopback 25578; registered `hub` loopback 25576; registered `game` loopback 25577.
- Crafty fixture HTTPS: loopback 8443, self-signed certificate accepted only in the disposable provider definition. Do not change production TLS policy.
- Provider id `crafty`, type `crafty`; game provider-server-id must be the newly imported fixture's UUID. Keep idle shutdown off for this transport acceptance probe.
- Create the fixture using Crafty's supported Java ZIP import. The 4.10.8 custom creation schema is advertised but its controller raises an unimplemented KeyError. Do not treat this as a HubPilot provider defect.
- Backend input archive contains the chosen Paper runtime, libraries and existing accepted EULA; no production plugins/configuration are required. Allow import to complete before adding test plugins.
- Paper probe depends on Hub, Link and Interact. It builds the actual telemetry inventory through the loaded Hub classloader and checks all three components are enabled. No mock server is used.
- Velocity probe depends on Core. It uses actual loaded Core configuration/provider transport and actual Velocity backend status pings, with no fabricated Player objects. It checks provider test, start acceptance, eventual status reachability, and stop acceptance. Verify actual process exit separately.
- Explicit guard: launch the proxy with `-Dhubpilot.readiness.lab=true -Dhubpilot.readiness.serverId=<fixture UUID>`. The probe rejects any other provider URL, a non-loopback backend or a mismatched server ID before power actions. This opt-in is not a production-safe plugin mode; the fixture must still be network-isolated.

The probe tests provider transport and platform integration, **not** full player admission, shared-start concurrency, cancellation or automatic stop. Those remain the 17 differential characterization tests; their existing failures are intentionally preserved. Never represent a provider 2xx or a status ping as successful player login.

## Human-assisted gate

An authenticated real Java client is required to sign off Mojang authentication, modern forwarding/UUID identity, owner claiming and Hub AUTH/STATE reports, Navigator/Interact request handling, countdown/sounds/transfer, cancellation feedback, admin-item visibility and reconnect behavior. The no-network fixture cannot authenticate or accept outside players. Stage a separately scoped private-access fixture with fresh forwarding/provider credentials for that session; do not expose these raw restored production copies.

Also test strict-version and forwarding-secret mismatch rejection. Record exact client, runtime and candidate hashes, not version labels alone. Do not fix lifecycle defects in this acceptance change.

## Optional offline protocol smoke

`client/` pins minecraft-protocol 1.68.0 and its full npm integrity lock; tested with Node 24.15.0. Run `npm ci --ignore-scripts` there in the disposable environment, then `HUBPILOT_ISOLATED_PROTOCOL_TEST=true node smoke.cjs` and `HUBPILOT_ISOLATED_PROTOCOL_TEST=true node direct-negative.cjs`. Both hardcode only the loopback fixture ports above. These files contain no production connection data or credentials.

Enable Paper modern forwarding using a fresh secret matching the fixture proxy, with both authentication flags deliberately offline for this isolated test. Positive: reach play through the proxy and verify the expected offline UUID in the fixture playerdata. Negative: direct backend connection must fail explicitly because Velocity is required, not merely because the port is closed. Follow with Core component/settings evidence while the carrier exists. Authenticated account identity remains a human-assisted gate.
