package dev.hubpilot.hub.telemetry;

import dev.hubpilot.hub.HubPilotHubPlugin;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class RichTelemetryStore {
   private static final Map<HubPilotHubPlugin, RichTelemetryStore> INSTANCES = new WeakHashMap<>();
   private final ConcurrentHashMap<String, RichTelemetryStore.Snapshot> rows = new ConcurrentHashMap<>();

   private RichTelemetryStore() {
   }

   public static synchronized RichTelemetryStore of(HubPilotHubPlugin var0) {
      return INSTANCES.computeIfAbsent(var0, var0x -> new RichTelemetryStore());
   }

   public void put(String var1, RichTelemetryStore.Snapshot var2) {
      if (var1 != null && var2 != null) {
         this.rows.put(var1.toLowerCase(Locale.ROOT), var2);
      }
   }

   public RichTelemetryStore.Snapshot get(String var1) {
      return var1 == null ? RichTelemetryStore.Snapshot.empty() : this.rows.getOrDefault(var1.toLowerCase(Locale.ROOT), RichTelemetryStore.Snapshot.empty());
   }

   public record Snapshot(
      long receivedAt,
      long backendPingMs,
      int queueSize,
      int startupProgress,
      long averageStartupSeconds,
      long totalRequests,
      long successfulJoins,
      long failedJoins,
      String lastError,
      boolean linkPresent,
      long linkHeartbeatAt,
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
      int linkPlayers,
      boolean ready
   ) {
      public static RichTelemetryStore.Snapshot empty() {
         return new RichTelemetryStore.Snapshot(0L, -1L, 0, 0, 0L, 0L, 0L, 0L, "", false, 0L, "", "", "", -1.0, -1.0, -1L, -1L, -1.0, -1, -1, -1, false);
      }

      public boolean fresh() {
         return this.receivedAt > 0L && System.currentTimeMillis() - this.receivedAt < 10000L;
      }

      public boolean linkFresh() {
         return this.linkPresent && this.linkHeartbeatAt > 0L && System.currentTimeMillis() - this.linkHeartbeatAt < 10000L;
      }
   }
}
