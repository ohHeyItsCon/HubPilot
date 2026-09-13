# HubPilot engineering handoff — September 13, 2026

Start with [ENGINEERING-HANDOFF.txt](ENGINEERING-HANDOFF.txt). It covers the four
Minecraft plugins, not the separate HubPilot Controller project.

The original complete build workspace was not recovered. This folder supplies
public release evidence and new inspection tooling, not a reproducible source build.
No private or pre-public source/archive has been restored here.

- [Plain-text handoff](ENGINEERING-HANDOFF.txt): implementation, protocol, providers,
  source gaps, provenance, validation limits, operations and roadmap.
- [Pinned public assets](public-artifacts.json): release URLs, hashes and dates.
- [Recovery tool](recover_public_release.py): verified public downloads, archive
  checks and optional inspection-only Java/resource recovery.
- [Direct bytecode evidence](BYTECODE-EVIDENCE.txt): selected current Core methods.
- [Audit results](AUDIT-RESULTS.txt): checks performed in this audit.

From the repository root, with Python 3.10+ and Java available:

```sh
python3 development/handoff-2026-09-13/recover_public_release.py \
  --output /tmp/hubpilot-public-1.0.2 --decompile
```

The tool creates `reconstructed-inspection-only.zip` locally. Its Java is decompiled
from the hash-pinned public JARs and has **not** been compiled. CFR can misrepresent
control flow; compare the original bytecode before making changes. This command
does not execute Minecraft plugins or modify a production installation.

The surviving replacement sources and two fixtures remain in
[development/1.0.2](../1.0.2/BUILD.md). Keep HubPilot, AutoServer and dependency license
notices when using or redistributing recovered material.
