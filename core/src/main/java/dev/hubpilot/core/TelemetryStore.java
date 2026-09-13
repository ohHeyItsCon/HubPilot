package dev.hubpilot.core;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

final class TelemetryStore {
   private final ConcurrentHashMap<String, TelemetryStore.Snapshot> byVelocityServer = new ConcurrentHashMap<>();

   void update(String var1, TelemetryStore.Snapshot var2) {
      if (var1 != null && var2 != null) {
         this.byVelocityServer.put(var1.toLowerCase(Locale.ROOT), var2);
      }
   }

   TelemetryStore.Snapshot get(String var1) {
      if (var1 == null) {
         return TelemetryStore.Snapshot.empty();
      } else {
         TelemetryStore.Snapshot var2 = this.byVelocityServer.get(var1.toLowerCase(Locale.ROOT));
         if (var2 == null) {
            return TelemetryStore.Snapshot.empty();
         } else {
            return System.currentTimeMillis() - var2.receivedAt() > 10000L ? var2.stale() : var2;
         }
      }
   }

   record Snapshot(
      long receivedAt,
      boolean linkPresent,
      String platform,
      String minecraftVersion,
      String linkVersion,
      double tps,
      double mspt,
      long memoryUsedMb,
      long memoryMaxMb,
      double cpuPercent,
      int loadedChunks,
      int entities,
      int players,
      boolean ready
   ) {
      static TelemetryStore.Snapshot empty() {
         return new TelemetryStore.Snapshot(0L, false, "", "", "", -1.0, -1.0, -1L, -1L, -1.0, -1, -1, -1, false);
      }

      TelemetryStore.Snapshot stale() {
         return new TelemetryStore.Snapshot(
            this.receivedAt,
            false,
            this.platform,
            this.minecraftVersion,
            this.linkVersion,
            this.tps,
            this.mspt,
            this.memoryUsedMb,
            this.memoryMaxMb,
            this.cpuPercent,
            this.loadedChunks,
            this.entities,
            this.players,
            this.ready
         );
      }
   }
}
