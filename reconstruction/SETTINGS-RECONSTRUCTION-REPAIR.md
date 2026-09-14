# Settings transport reconstruction repair

## Evidence and distinction

An offline real-client acceptance run reached Paper through Velocity but repeatedly logged a ClassCastException in reconstructed `SettingsSyncHook.handle`. This is a reconstruction regression, not a defect being silently fixed in the published lifecycle baseline.

Direct `javap -p -c` on the pinned public Core shows:

- Offset 21 returns the identifier Object; offset 24 stores it without a Properties checkcast.
- Offset 65 returns the source Object; offset 68 stores it without a Path checkcast.
- Offset 221 casts the payload to byte[] (`[B`); offsets 235 and 243 use arraylength directly.

Reconstructed source had merged reused local-variable slots: identifier with later Properties, source with later Path, and byte[] with an Object-array length expression. All three could fail at runtime despite compilation/class verification. Split those locals and retain the original control flow, allowlist, trusted-server checks, size limits, replacement semantics, atomic-write fallback and reload behavior. No lifecycle logic, permissions policy or protocol is changed.

## Differential regression

`python3 reconstruction/settings-test.py --jdk <reference-jdk>` runs identical tests in separate JVMs against pinned public Core and candidate. Seven cases cover unrelated-channel pass-through, trusted framed snapshot/allowlist/reload, equal-snapshot no-op, untrusted source rejection, client-like source rejection, empty/oversized/malformed data, and replacement rather than merge.

Before repair: public Core passes all seven; checkpoint-derived candidate fails the unrelated-channel case. After repair: both must pass with identical output. Live repeat must confirm actual settings application and absence of the prior ClassCastException. The test fixture uses actual channel identifiers and Java binary framing, not source-pattern assertions.

The preserved `ea5f439` checkpoint remains useful provenance but is now known to contain this transport regression and must not be deployed. This correction belongs in its own reconstruction-repair commit, separate from the CHAIN compatibility fix and proposed lifecycle defects.
