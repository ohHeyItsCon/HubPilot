# Separate proposals — not applied to reconstruction

The reconstruction intentionally preserves the public baseline. Tests named `existing-behavior-*` assert its observable behavior, including undesirable outcomes. Do not silently turn those assertions into desired behavior without a separately scoped change.

## Reproduced against both public and reconstructed Core (isolated harness)

1. **Late ping after cancel:** cancelling before the initial ping fails does not invalidate that callback; it can create a new queue/start. Proposal: player request generations captured/revalidated through asynchronous boundaries.
2. **Delayed retry after cancellation/new destination:** an active player can be sent toward a previously requested target after a successful later selection. Proposal: retain/cancel retry tasks and validate current request generation/target immediately before connection.
3. **Stale admission settings:** retry uses captured definition after maintenance is enabled; fresh requests correctly reject maintenance. Proposal: authoritative current-config revalidation for retries/countdown completion, with explicit handling of revoked permissions.
4. **Competing demand:** an exhausted failed online join can issue provider stop while another player is queued to start that same backend. Proposal: serialized per-backend lifecycle/demand accounting and final current-demand guard, including startup ownership and pending attempts.
5. **Outstanding start after empty-queue cleanup:** stop is issued while start HTTP remains outstanding; start later completes after queue removal. Proposal: generation-aware command state and unknown-outcome reconciliation; avoid blind HTTP replay.
6. **One UUID across servers:** one player can create independent startup queues on different backends. Proposal: explicit global per-player request policy as part of approved startup-abuse controls, preserving shared starts and no charge for online joins.
7. **Stale non-hub Always-On:** current effective config suppresses automatic shutdown, but an old captured definition passed to final cleanup still permits stop. Proposal: recheck latest effective Always-On and demand rules at the automatic dispatcher; preserve the existing stronger configured-hub protection.

## Preserved protections proven in the same harness

Concurrent shared requests coalesce into one provider start; duplicate UUIDs do not duplicate a server queue. Cancelling one requester preserves another. Configured-hub protection consults the current mapping even for a stale passed definition. Connected proxy players block automatic stop. Idle checks preserve queued and pending transfers. Eligible ordinary idle servers still stop. Online joins bypass startup countdown; countdown completion fans out the queued connection attempts.

## Still hypotheses / separate compatibility work

Direct players and other proxies are not represented by this proxy's local population guard. Cross-store reload atomicity, overlapping same-player pending-transfer counts, every stop/start interleaving, shutdown callbacks, settings snapshot conflicts, discovery endpoint refresh and adversarial transport payloads need additional tests. No production occurrence is asserted.

Hub's baseline `Material.CHAIN` reference is version-sensitive on Paper 1.21.10. A compatibility fallback/change is a distinct fix, not part of source restoration. Preserve genuine platform constraints rather than fabricating a successful linkage result.

No release, deployment, admission policy change, production auto-stop change or Controller integration is authorized by this proposal document. Preserve Crafty-first Prepare for Velocity as the committed major roadmap direction.
